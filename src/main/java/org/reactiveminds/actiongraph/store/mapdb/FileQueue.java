package org.reactiveminds.actiongraph.store.mapdb;

import akka.dispatch.Envelope;
import org.mapdb.DB;
import org.reactiveminds.actiongraph.store.QueueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class FileQueue implements QueueStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileQueue.class);
    private final BlockingQueue<Envelope> delegate;
    private final DB db;

    public String getName() {
        return name;
    }

    @Override
    public boolean offer(Envelope envelope, Duration block) {
        try {
            return offer(envelope, block.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private final String name;
    public FileQueue(String name, BlockingQueue<Envelope> delegate, DB db) {
        this.delegate = delegate;
        this.db = db;
        this.name = name;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void clear() {
        delegate.clear();
        db.commit();
    }

    private Envelope poll() {
        Envelope poll = delegate.poll();
        db.commit();
        return poll;
    }
    public void flush(){
        if(!db.isClosed()) {
            db.commit();
            LOGGER.debug("flushing queue {}", getName());
        }
    }
    public Envelope poll(boolean flush) {
        if(flush)
            return poll();
        else
            return delegate.poll();
    }

    private boolean offer(Envelope handle, long timeout, TimeUnit unit) throws InterruptedException {
        boolean b = delegate.offer(handle, timeout, unit);
        if(b)
            db.commit();
        return b;
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
