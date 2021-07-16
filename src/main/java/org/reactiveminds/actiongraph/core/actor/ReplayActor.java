package org.reactiveminds.actiongraph.core.actor;

import akka.actor.Props;
import akka.dispatch.BoundedMessageQueueSemantics;
import akka.dispatch.RequiresMessageQueue;
import akka.serialization.Serialization;
import org.reactiveminds.actiongraph.core.AbstractNode;
import org.reactiveminds.actiongraph.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

class ReplayActor extends NodeActor implements RequiresMessageQueue<BoundedMessageQueueSemantics> {
    private Logger logger = LoggerFactory.getLogger(ReplayActor.class);
    /**
     * @param node
     * @param running
     */
    private int index;
    ReplayActor(AbstractNode node, AtomicBoolean running, int index) {
        super(node, running);
        this.index = index;
        logger.debug("retry[{}] actor created, for {}",index, node.path());
    }
    public static Props create(AbstractNode node, AtomicBoolean running, int index) {
        return Props.create(ReplayActor.class, node, running, index);
    }

    protected void replay(Command.ReplayCommand event) {
        try {
            Timer timer = new Timer();
            timer.halt(event.originTime, event.delay);
            if(event.originType == 1)
                walkTree(event);
            else if(event.originType == 2)
                fireAction(event);
            else {
                logger.warn("something going unhandled! {}", event);
                unhandled(event);
            }
        }
        finally {
            BoundedPersistentMailbox.flush(getSelf());
        }
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.ReplayCommand.class, event -> replay(event))
                .build();
    }
    public void preStart() throws Exception {
        logger.debug("retry actor started, at {}", Serialization.serializedActorPath(getSelf()));
        int nextIndex = index - 1;
        if(nextIndex > 0){
            replayActor = getContext().actorOf(ReplayActor.create(node, running, nextIndex), node.path().replaceAll("/", "\\.") + "-r" + nextIndex);
        }
    }
}
