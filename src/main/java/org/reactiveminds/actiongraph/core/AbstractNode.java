package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.ActionGraphException;
import org.reactiveminds.actiongraph.Node;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;
import java.util.logging.Logger;

abstract class AbstractNode implements Node {
    private static final Logger LOG = Logger.getLogger(AbstractNode.class.getName());
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
        LOG.fine(String.format("deleted %s node '%s'",type(), name()));
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
        actorWrapper = Actors.instance().actorOf(path(), this);
    }
    protected final ActorReference actorWrapper;
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
    final void stopActor(){
        actorWrapper.tell(new NodeActor.StopEvent(), null);
        try {
            actorWrapper.awaitTermination(Duration.ofSeconds(30));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    abstract void react(Predicate<Node> filter, Serializable signal);
}
