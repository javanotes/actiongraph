package org.reactiveminds.actiongraph.store.mapdb;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.Bootstrap;
import org.reactiveminds.actiongraph.store.*;
import org.reactiveminds.actiongraph.store.QueueStore;
import org.reactiveminds.actiongraph.store.StoreProvider;
import org.reactiveminds.actiongraph.util.SystemProps;
import org.reactiveminds.actiongraph.util.err.ActionGraphException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DefaultStoreProvider extends AbstractEventJournal implements StoreProvider,EventJournal {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStoreProvider.class);
    static final String GROUPS = "GROUPS";
    static final String ACTIONS = "ACTIONS";
    private DB mapDB;
    private HTreeMap<String, ActionData> actionDB;
    private HTreeMap<String, GroupData> groupDB;
    private HTreeMap<String, ActionEntries> eventJournal;
    private File createDB(){
        String dbPath = System.getProperty(SystemProps.DB_FILE_PATH);
        if(dbPath == null)
            throw new ActionGraphException("Mandatory property missing '"+SystemProps.DB_FILE_PATH+"'. Cannot create db ");
        File file = new File(dbPath);
        mapDB = DBMaker.newFileDB(file)
                //.mmapFileEnableIfSupported()
                .make();

        groupDB = mapDB.createHashMap(GROUPS)
                .keySerializer(Serializer.STRING)
                .valueSerializer(new GroupDataSer())
                .makeOrGet();

        actionDB = mapDB.createHashMap(ACTIONS)
                .keySerializer(Serializer.STRING)
                .valueSerializer(new ActionDataSer())
                .makeOrGet();

        eventJournal = mapDB.createHashMap("EVENTS")
                .keySerializer(Serializer.STRING)
                .valueSerializer(new ActionEntrySerializer())
                .makeOrGet();
        for (int i=0; i<MUTEX_BUCKETS.length; i++){
            MUTEX_BUCKETS[i] = new Object();
        }
        startJournalCleaner();
        return file;
    }

    private void startJournalCleaner() {
        Thread t = new Thread("journal-cleaner"){
            @Override
            public void run(){
                boolean running = true;
                while (running){
                    try {
                        QueuedEntry entry = delayQueue.take();
                        eventJournal.remove(entry.corrId);
                        LOGGER.info("removed journal entry {}", entry.corrId);
                    } catch (InterruptedException e) {
                        running = false;
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private File dbFile;

    @Override
    public EventJournal getEventJournal() {
        return this;
    }

    @Override
    public void open() {
        dbFile = createDB();
        LOGGER.info("File store opened at: {}", dbFile.getAbsolutePath());
    }

    private QueueStore mapDBQueue(String name, int size){
        String qName = "CQ_"+name;
        if(mapDB.isClosed()){
            LOGGER.warn("* getMailboxQueue called when db is closed! If this is on shutdown, can be ignored");
            return new FileQueue(qName, new ArrayBlockingQueue<>(size), mapDB);
        }
        LOGGER.debug("getting qname: {}",qName);
        return mapDB.exists(qName) ? new FileQueue(qName, mapDB.getCircularQueue(qName), mapDB)
                : new FileQueue(qName, mapDB.createCircularQueue(qName, new EnvelopeSerializer(), size), mapDB);
    }
    private QueueStore tapeQueue(String name){
        return new TapeQueue(name + ".q", dbFile.getParentFile());
    }
    private List<QueueStore> queueStores = Collections.synchronizedList(new ArrayList<>());
    @Override
    public QueueStore getMailBox(String name, int size) {
        QueueStore queueStore = tapeQueue(name);
        queueStores.add(queueStore);
        return queueStore;
    }

    @Override
    public ActionData loadAction(String actionId) {
        return actionDB.get(actionId);
    }

    @Override
    public List<ActionData> loadAllActions() {
        return new ArrayList<>(actionDB.values());
    }

    @Override
    public GroupData loadGroup(String groupId) {
        return groupDB.get(groupId);
    }

    @Override
    public boolean groupExists(String root) {
        return groupDB.containsKey(root);
    }

    @Override
    public boolean actionExists(String path) {
        return actionDB.containsKey(path);
    }

    @Override
    public List<GroupData> loadAllGroups() {
        return new ArrayList<>(groupDB.values());
    }

    @Override
    public void save(String id, ActionData actionData) {
        actionDB.put(id, actionData);
        mapDB.commit();
    }

    @Override
    public void save(String id, GroupData groupData) {
        groupDB.put(id, groupData);
        mapDB.commit();
    }

    @Override
    public void close() throws IOException {
        queueStores.forEach(queueStore -> {
            try {
                queueStore.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if(!mapDB.isClosed()) {
            mapDB.commit();
            mapDB.close();
        }
    }
    private static final long journalExpirySecs = Long.parseLong(System.getProperty(SystemProps.JOURNAL_EXPIRY, SystemProps.JOURNAL_EXPIRY_DEFAULT));
    static class QueuedEntry implements Delayed{
        private final String corrId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueuedEntry that = (QueuedEntry) o;
            return corrId.equals(that.corrId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(corrId);
        }

        private final long updated;

        QueuedEntry(String corrId, long updated) {
            this.corrId = corrId;
            this.updated = updated;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delay = updated + Duration.ofSeconds(journalExpirySecs).toMillis() - System.currentTimeMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
        }
    }
    private DelayQueue<QueuedEntry> delayQueue =  new DelayQueue<>();

    @Override
    protected void recordEvent(ActionEntries entry) {
        eventJournal.put(entry.getCorrelationId(), entry);
        mapDB.commit();
        queueEntry(entry);
    }

    @Override
    protected ActionEntries fetchEvent(String correlationId) {
        return null;
    }

    private void queueEntry(ActionEntry entry){
        QueuedEntry queuedEntry = new QueuedEntry(entry.getCorrelationId(), entry.getUpdated());
        delayQueue.remove(queuedEntry);
        delayQueue.add(queuedEntry);
    }

    private final Object[] MUTEX_BUCKETS = new Object[Integer.parseInt(System.getProperty(SystemProps.MUTEX_BUCKETS, SystemProps.MUTEX_BUCKETS_DEFAULT)) ];


}
