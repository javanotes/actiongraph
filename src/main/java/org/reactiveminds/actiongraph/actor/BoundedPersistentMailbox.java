package org.reactiveminds.actiongraph.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.actor.ActorSystem;
import akka.dispatch.*;
import akka.serialization.Serialization;
import com.typesafe.config.Config;
import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.react.Predicates;
import scala.Option;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public class BoundedPersistentMailbox implements MailboxType, ProducesMessageQueue<PersistentMessageQueue> {
    private int bufferSize;
    private long pushTimeoutMs;
    private int maxRetry;
    private double backoff;
    private final ActorRefProvider provider;
    public BoundedPersistentMailbox(ActorSystem.Settings settings, Config config) {
        bufferSize = config.getInt("mailbox-capacity");
        pushTimeoutMs = config.getLong("mailbox-push-timeout-ms");
        maxRetry = config.getInt("mailbox-push-retry");
        backoff = config.getDouble("mailbox-push-retry-backoff");
        provider = Actors.instance().serialization().system().provider();
    }

    @Override
    public MessageQueue create(Option<ActorRef> owner, Option<ActorSystem> system) {
        PersistentMessageQueue messageQueue = new PersistentMessageQueue(bufferSize, pushTimeoutMs, provider);
        messageQueue.setBackoff(backoff);
        messageQueue.setMaxRetry(maxRetry);
        return messageQueue;
    }

}
