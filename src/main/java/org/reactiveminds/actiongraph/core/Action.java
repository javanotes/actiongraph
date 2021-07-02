package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.ActionGraphException;
import org.reactiveminds.actiongraph.Node;
import org.reactiveminds.actiongraph.react.Reaction;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;

public class Action extends AbstractNode{

    protected String content;
    private final List<Reaction> subscribers = new LinkedList<>();
    Action(Group parent, String name, ReadWriteLock readWriteLock) {
        super(parent, name, readWriteLock);
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new ActionGraphException("internal error!", e);
        }
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
    public String read(){
        readWriteLock.readLock().lock();
        try{
            if(isDeleted)
                throw new ActionGraphException("File is deleted!");
            return content;
        }finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * Single entry key-value
     * Key - checksum, Value - content
     * @return
     */
    public Map<String, String> readWithChecksum(){
        readWriteLock.readLock().lock();
        try{
            if(isDeleted)
                throw new ActionGraphException("File is deleted!");
            return Collections.singletonMap(digest(), content);
        }finally {
            readWriteLock.readLock().unlock();
        }
    }
    private final MessageDigest digest;
    private synchronized String digest(){
        digest.reset();
        return Base64.getEncoder().encodeToString(digest.digest(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8)));
    }
    public void write(String content){
        readWriteLock.writeLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("File is deleted!");
            this.content = content;
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }
    public void compareAndWrite(String checksum, String content){
        readWriteLock.writeLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("File is deleted!");
            if(digest().equals(checksum))
                this.content = content;
        }
        finally {
            readWriteLock.writeLock().unlock();
        }
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
    public void react(Predicate<Node> filter, Serializable signal) {
        if(filter.test(this)){
            // this is invoked in actor - thread safe
            subscribers.forEach(reaction -> reaction.accept(this, signal));
        }
    }
}
