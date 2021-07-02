package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.ActionGraphException;
import org.reactiveminds.actiongraph.Node;
import org.reactiveminds.actiongraph.react.Reaction;
import org.reactiveminds.actiongraph.react.ReactiveOperationException;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;

public class Action extends AbstractNode{

    protected String content;
    private final Observable observable = new Observable(){
        @Override
        public void notifyObservers(Object arg){
            setChanged();
            super.notifyObservers(arg);
        }
    };
    Action(Group parent, String name, ReadWriteLock readWriteLock) {
        super(parent, name, readWriteLock);
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new ReactiveOperationException("internal error!", e);
        }
    }

    /**
     * Add observer
     * @param instance
     * @param <T>
     * @return
     */
    public <T extends Reaction> Action addObserver(final T instance){
        observable.addObserver((o, arg) -> instance.accept(this, (Serializable) arg));
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
            observable.deleteObservers();
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
        if(filter.test(this))
            observable.notifyObservers(signal);
    }
}
