package org.reactiveminds.actiongraph.core;

import akka.actor.ActorRef;
import akka.pattern.AskableActorRef;
import org.reactiveminds.actiongraph.Node;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;

abstract class AbstractNode implements Node {
    private static final Logger LOG = Logger.getLogger(AbstractNode.class.getName());
    protected final ReadWriteLock readWriteLock;
    boolean isDeleted = false;
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
        LOG.info(String.format("deleted %s node '%s'",type(), name()));
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

    AbstractNode(Group parent, String name, ReadWriteLock readWriteLock) {
        this.parent = parent;
        this.name = name;
        this.readWriteLock = readWriteLock;
        id = UUID.randomUUID().toString();
        created = System.currentTimeMillis();
        modified = created;
        actorWrapper = Actors.instance().actorOf(path(), this);
        askableActorRef = new AskableActorRef(actorWrapper.actorRef);
    }
    protected final Actors.ActorWrapper actorWrapper;
    private final AskableActorRef askableActorRef;
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
        actorWrapper.actorRef.tell(new NodeActor.StopEvent(), ActorRef.noSender());
        try {
            actorWrapper.awaitTermination(Duration.ofSeconds(30));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
