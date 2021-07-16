package org.reactiveminds.actiongraph.store;

import akka.dispatch.Envelope;

import java.io.Closeable;
import java.time.Duration;

/**
 * File backed queue
 */
public interface QueueStore  extends Closeable {
    String getName();
    boolean offer(Envelope envelope, Duration block);
    Envelope poll(boolean flush);
    void flush();
    int size();
    void clear();
}
