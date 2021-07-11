package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.core.actor.ActorReference;
import org.reactiveminds.actiongraph.core.actor.Actors;
import org.reactiveminds.actiongraph.core.actor.Event;
import org.reactiveminds.actiongraph.react.ActionMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;

public abstract class AbstractNode implements Node {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNode.class);
    protected final ReadWriteLock readWriteLock;
    volatile boolean isDeleted = false;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractNode that = (AbstractNode) o;
        return type() == that.type() &&
                Objects.equals(id, that.id);
    }

    void unlink(){
        if(parent != null) {
            parent.children.remove(new TimedString(name()));
            parent = null;
        }
    }
    @Override
    public boolean delete(){
        stopActor();
        unlink();
        isDeleted = true;
        LOG.debug(String.format("deleted %s node '%s'",type(), name()));
        return isDeleted;
    }
    @Override
    public String toString() {
        return "{" +
                "type=" + type() +
                ", name='" + name + '\'' +
                ", created=" + created +
                ", modified=" + modified +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), id);
    }
    static void checkName(String name){
        if(name == null || name.trim().isEmpty() || name.contains("/") || name.contains("."))
            throw new ActionGraphException("node name not allowed '"+name+"'");
    }
    AbstractNode(Group parent, String name, ReadWriteLock readWriteLock) {
        checkName(name);
        this.parent = parent;
        this.name = name;
        this.readWriteLock = readWriteLock;
        id = UUID.randomUUID().toString();
        created = System.currentTimeMillis();
        modified = created;
        actorReference = Actors.instance().actorOf(path(), this);
    }

    public ActorReference getActorReference() {
        return actorReference;
    }

    protected final ActorReference actorReference;
    @Override
    public Group parent() {
        return parent;
    }

    protected Group parent;
    protected final String name;
    protected final String id;
    protected final long created;
    protected long modified;


    @Override
    public String name() {
        return name;
    }

    @Override
    public String path() {
        return parent == null ? "/"+name() : parent.path() + "/" + name();
    }
    public boolean exists(){
        readWriteLock.readLock().lock();
        try{
            return !isDeleted;
        }finally {
            readWriteLock.readLock().unlock();
        }
    }
    public void stopActorChain(){
        LOG.debug("Stopping actor on: "+path());
        if(type() == Node.Type.GROUP){
            Group branchNode = (Group) this;
            branchNode.readWriteLock.readLock().lock();
            try{
                branchNode.children.entrySet().stream()
                        .forEach(e -> {
                            AbstractNode value = (AbstractNode) e.getValue();
                            value.stopActor();
                        });
            }finally {
                branchNode.readWriteLock.readLock().unlock();
            }
        }
    }
    private final void stopActor(){
        actorReference.tell(Event.newEvent(Event.STOP, null, null));
        try {
            actorReference.awaitTermination(Duration.ofSeconds(30));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public abstract void react(ActionMatcher filter, String signal);
}
