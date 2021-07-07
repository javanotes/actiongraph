package org.reactiveminds.actiongraph.node;

import org.reactiveminds.actiongraph.ActionGraphException;
import org.reactiveminds.actiongraph.react.Reaction;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;

public class Action extends AbstractNode{

    private final List<Reaction> subscribers = new LinkedList<>();
    Action(Group parent, String name, ReadWriteLock readWriteLock) {
        super(parent, name, readWriteLock);
    }

    /**
     * Add observer
     * @param instance
     * @param <T>
     * @return
     */
    public synchronized <T extends Reaction> Action  addObserver(final T instance){
        subscribers.add(instance);
        return this;
    }
    public synchronized <T extends Reaction> boolean removeObserver(final T instance){
        return subscribers.remove(instance);
    }

    @Override
    public boolean delete() {
        readWriteLock.writeLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("File is deleted!");
            subscribers.forEach(reaction -> reaction.destroy());
            subscribers.clear();
            return super.delete();
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Type type() {
        return Type.ACTION;
    }

    public final void react(Predicate<Node> filter, String signal) {
        // this is invoked in actor - thread safe
        subscribers.forEach(reaction -> reaction.accept(path(), signal));
    }
}
