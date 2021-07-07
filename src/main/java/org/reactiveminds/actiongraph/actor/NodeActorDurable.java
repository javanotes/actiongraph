package org.reactiveminds.actiongraph.actor;

import akka.actor.Props;
import akka.dispatch.BoundedMessageQueueSemantics;
import akka.dispatch.RequiresMessageQueue;
import org.reactiveminds.actiongraph.node.AbstractNode;

import java.util.concurrent.atomic.AtomicBoolean;

class NodeActorDurable extends NodeActor implements RequiresMessageQueue<BoundedMessageQueueSemantics> {
    /**
     * @param node
     * @param running
     */
    NodeActorDurable(AbstractNode node, AtomicBoolean running) {
        super(node, running);
    }
    public static Props create(AbstractNode node, AtomicBoolean running) {
        return Props.create(NodeActorDurable.class, node, running);
    }
}
