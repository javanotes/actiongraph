package org.reactiveminds.actiongraph.core.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefProvider;
import akka.actor.ActorSystem;
import akka.dispatch.*;
import akka.serialization.Serialization;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

import java.util.concurrent.ConcurrentHashMap;

public class BoundedPersistentMailbox implements MailboxType, ProducesMessageQueue<PersistentMessageQueue> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoundedPersistentMailbox.class);
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
        LOGGER.debug("Using persistent mailbox of size {}, push timeout {} ms", bufferSize, pushTimeoutMs);
    }
    private static ConcurrentHashMap<String, PersistentMessageQueue> mailboxInstances = new ConcurrentHashMap<>();
    private static void flush(String actorPath){
        if(mailboxInstances.containsKey(actorPath)) {
            mailboxInstances.get(actorPath).flush();
        }
    }
    final static void flush(ActorRef actorRef){
        flush(actorRef.path().name());
    }
    @Override
    public MessageQueue create(Option<ActorRef> owner, Option<ActorSystem> system) {
        PersistentMessageQueue messageQueue = new PersistentMessageQueue(owner.get(), bufferSize, pushTimeoutMs, provider);
        messageQueue.setBackoff(backoff);
        messageQueue.setMaxRetry(maxRetry);
        if(!owner.isEmpty()){
            String actorPath = owner.get().path().name();
            mailboxInstances.put(actorPath, messageQueue);
            LOGGER.debug("Mailbox mapped for {}",actorPath);
        }
        return messageQueue;
    }

}
