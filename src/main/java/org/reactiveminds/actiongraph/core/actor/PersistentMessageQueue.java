package org.reactiveminds.actiongraph.core.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.dispatch.BoundedMessageQueueSemantics;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PersistentMessageQueue implements MessageQueue, BoundedMessageQueueSemantics {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentMessageQueue.class);
    private BlockingQueue<Envelope> blockingQueue = null;
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
    public PersistentMessageQueue(int size, long pushTimeoutMs, ActorRefProvider actorRefProvider) {
        this.size = size;
        this.actorRefProvider = actorRefProvider;
        this.pushTimeoutMs = pushTimeoutMs;
    }
    private AtomicInteger runningCount = new AtomicInteger();
    private void getQueue(ActorRef receiver){
        if (blockingQueue == null) {
            synchronized (this) {
                if (blockingQueue == null) {
                    blockingQueue = GraphStore.getMailboxQueue(receiver.path().name(), new EnvelopeSerializer(), size);
                    LOGGER.info("Getting mailbox for actor path: {}, ", receiver.path().name());
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
            try {
                offer = blockingQueue.offer(handle, timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
        if(blockingQueue == null)
            return null;
        else {
            Envelope poll = blockingQueue.poll();
            if(poll != null)
                runningCount.decrementAndGet();
            return poll;
        }
    }

    @Override
    public int numberOfMessages() {
        return runningCount.get();
    }

    @Override
    public boolean hasMessages() {
        return numberOfMessages() > 0;
    }

    @Override
    public void cleanUp(ActorRef owner, MessageQueue deadLetters) {
        while (!blockingQueue.isEmpty()){
            deadLetters.enqueue(owner, blockingQueue.poll());
        }
    }

    @Override
    public Duration pushTimeOut() {
        return Duration.apply(pushTimeoutMs, TimeUnit.MILLISECONDS);
    }
}
