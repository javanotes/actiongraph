package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.react.ActionMatcher;
import org.reactiveminds.actiongraph.react.Reaction;
import org.reactiveminds.actiongraph.util.err.ActionGraphException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

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
    @Override
    public final boolean react(String correlationId, ActionMatcher filter, String signal) {
        // this is invoked in actor - thread safe
        boolean fired = false;
        for (Reaction reaction : subscribers) {
            reaction.accept(path(), signal);
            fired = true;
        }
        return fired;
    }
}
