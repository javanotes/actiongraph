package org.reactiveminds.actiongraph.store;

import akka.dispatch.Envelope;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * A transient {@link StoreProvider}, probably should be used for demo purpose only
 */
public class InMemoryStoreProvider extends AbstractEventJournal implements StoreProvider{
    private ConcurrentMap<String, ActionData> actionDataConcurrentMap;
    private ConcurrentMap<String, GroupData> groupDataConcurrentMap;
    private ConcurrentMap<String, ActionEntries> actionEntriesConcurrentMap;
    @Override
    public EventJournal getEventJournal() {
        return this;
    }

    @Override
    public void open() {
        actionDataConcurrentMap = new ConcurrentHashMap<>();
        groupDataConcurrentMap = new ConcurrentHashMap<>();
        actionEntriesConcurrentMap = new ConcurrentHashMap<>();
    }

    @Override
    public QueueStore getMailBox(String name, int size) {
        return new QueueStore() {
            private BlockingQueue<Envelope> blockingQueue = new ArrayBlockingQueue<>(size);
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean offer(Envelope envelope, Duration block) {
                try {
                    return blockingQueue.offer(envelope, block.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return false;
            }

            @Override
            public Envelope poll(boolean flush) {
                if(flush)
                    return blockingQueue.poll();
                else
                    return blockingQueue.peek();

            }

            @Override
            public void flush() {
                blockingQueue.poll();
            }

            @Override
            public int size() {
                return blockingQueue.size();
            }

            @Override
            public void clear() {
                blockingQueue.clear();
            }

            @Override
            public void close() {
                clear();
            }
        };
    }

    @Override
    public ActionData loadAction(String actionId) {
        return actionDataConcurrentMap.get(actionId);
    }

    @Override
    public List<ActionData> loadAllActions() {
        return new ArrayList<>(actionDataConcurrentMap.values());
    }

    @Override
    public GroupData loadGroup(String groupId) {
        return groupDataConcurrentMap.get(groupId);
    }

    @Override
    public boolean groupExists(String root) {
        return groupDataConcurrentMap.containsKey(root);
    }

    @Override
    public boolean actionExists(String path) {
        return actionDataConcurrentMap.containsKey(path);
    }

    @Override
    public List<GroupData> loadAllGroups() {
        return new ArrayList<>(groupDataConcurrentMap.values());
    }

    @Override
    public void save(String id, ActionData actionData) {
        actionDataConcurrentMap.put(id, actionData);
    }

    @Override
    public void save(String id, GroupData groupData) {
        groupDataConcurrentMap.put(id, groupData);
    }

    @Override
    public void close() {

    }

    @Override
    protected void recordEvent(ActionEntries entry) {
        actionEntriesConcurrentMap.put(entry.getCorrelationId(), entry);
    }

    @Override
    protected ActionEntries fetchEvent(String correlationId) {
        return actionEntriesConcurrentMap.get(correlationId);
    }
}
