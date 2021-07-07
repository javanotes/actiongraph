package org.reactiveminds.actiongraph.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import org.reactiveminds.actiongraph.node.AbstractNode;
import org.reactiveminds.actiongraph.node.Group;
import org.reactiveminds.actiongraph.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

class NodeActor extends AbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(NodeActor.class);
    private final AbstractNode node;
    private final AtomicBoolean running;

    /**
     *
     * @param node
     * @param running
     */
    NodeActor(AbstractNode node, AtomicBoolean running) {
        this.node = node;
        this.running = running;
    }

    public static Props create(AbstractNode node, AtomicBoolean running) {
        return Props.create(NodeActor.class, node, running);
    }
    private void fireAction(Event event){
        if(event.predicate.test(node)) {
            node.react(event.predicate, event.payload);
        }
    }
    private void walkTree(Event event){
        Group branchNode = (Group) node;
        branchNode.walk(node -> {
            if(node.type() == Node.Type.GROUP) {
                node.getActorReference().tell(Event.newEvent(Event.GROUP, event.payload, event.predicate));
            }
            else {
                node.getActorReference().tell(Event.newEvent(Event.ACTION, event.payload, event.predicate));
            }
        });
    }
    private void stopReactors(){
        node.stopActorChain();
        getContext().stop(getSelf());
    }
    @Override
    public void postStop() throws Exception {
        super.postStop();
        synchronized (running){
            running.set(false);
            running.notifyAll();
        }
    }
    @Override
    public void preStart() throws Exception {
        super.preStart();
        synchronized (running){
            running.set(true);
            running.notifyAll();
        }
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Event.BranchEvent.class, event -> walkTree(event))
                .match(Event.LeafEvent.class, event -> fireAction(event))
                .match(Event.StopEvent.class, event -> stopReactors())
                .build();
    }
}
