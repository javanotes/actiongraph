package org.reactiveminds.actiongraph.core.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.dispatch.BoundedMessageQueueSemantics;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.reactiveminds.actiongraph.store.QueueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PersistentMessageQueue implements MessageQueue, BoundedMessageQueueSemantics {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentMessageQueue.class);
    private QueueStore fileQueue = null;
    private final int size;
    private final long pushTimeoutMs;
    private final ActorRefProvider actorRefProvider;

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public double getBackoff() {
        return backoff;
    }

    public void setBackoff(double backoff) {
        this.backoff = backoff;
    }

    private int maxRetry;
    private double backoff;
    public PersistentMessageQueue(ActorRef actorRef, int size, long pushTimeoutMs, ActorRefProvider actorRefProvider) {
        this.size = size;
        this.actorRefProvider = actorRefProvider;
        this.pushTimeoutMs = pushTimeoutMs;
        getQueue(actorRef);
    }
    private AtomicInteger runningCount = new AtomicInteger();
    private void getQueue(ActorRef receiver){
        if (fileQueue == null) {
            synchronized (this) {
                if (fileQueue == null) {
                    fileQueue = GraphStore.getMailboxQueue(receiver.path().name(), size);
                    LOGGER.debug("Getting mailbox '{}' for {} ", fileQueue.getName(), receiver);
                }
            }

        }
    }

    @Override
    public void enqueue(ActorRef receiver, Envelope handle) {
        getQueue(receiver);
        int retry = 0;
        boolean offer = false;
        long timeout = pushTimeoutMs;
        while (!offer && retry++ < maxRetry){
            offer = fileQueue.offer(handle, java.time.Duration.ofMillis(timeout));
            timeout += Double.valueOf(backoff*pushTimeoutMs).longValue();
        }
        if(offer)
            runningCount.incrementAndGet();
        else {
            ActorReference.deadLetter(handle.message());
            LOGGER.warn("dead letter published after push retry was exhausted!");
        }
    }


    @Override
    public Envelope dequeue() {
        if(fileQueue == null)
            return null;
        else {
            Envelope poll = fileQueue.poll(false);
            if(poll != null)
                runningCount.decrementAndGet();
            return poll;
        }
    }

    void flush(){
        LOGGER.debug("flushing queue {}", fileQueue.getName());
        fileQueue.flush();
    }
    @Override
    public int numberOfMessages() {
        return fileQueue.size();
    }

    @Override
    public boolean hasMessages() {
        return numberOfMessages() > 0;
    }

    @Override
    public void cleanUp(ActorRef owner, MessageQueue deadLetters) {
        while (!(fileQueue.size() == 0)){
            deadLetters.enqueue(owner, fileQueue.poll(true));
        }
    }

    @Override
    public Duration pushTimeOut() {
        return Duration.apply(pushTimeoutMs, TimeUnit.MILLISECONDS);
    }
}
