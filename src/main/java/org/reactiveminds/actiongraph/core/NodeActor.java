package org.reactiveminds.actiongraph.core;

import akka.actor.AbstractActor;
import akka.actor.Props;
import org.reactiveminds.actiongraph.Node;
import org.reactiveminds.actiongraph.react.Predicates;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.logging.Logger;

class NodeActor extends AbstractActor {
    private static final Logger LOG = Logger.getLogger(NodeActor.class.getName());
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
    public static Event BranchEvent(Serializable payload, Predicate<Node> predicate){
        return new BranchEvent(payload, predicate);
    }
    public static Event LeafEvent(Serializable payload, Predicate<Node> predicate){
        return new LeafEvent(payload, predicate);
    }
    public static abstract class Event{
        public final Serializable payload;
        public final Predicate<Node> predicate;
        private Event(Serializable payload, Predicate<Node> predicate) {
            this.payload = payload;
            this.predicate = predicate;
        }
    }
    static class BranchEvent extends Event{

        private BranchEvent(Serializable payload, Predicate<Node> predicate) {
            super(payload, predicate);
        }
    }
    static class LeafEvent extends Event{

        private LeafEvent(Serializable payload, Predicate<Node> predicate) {
            super(payload, predicate);
        }
    }
    static class StopEvent extends Event{

        public StopEvent() {
            super("!#BANG", Predicates.MATCH_ALL);
        }
    }
    private void recurse(Event event){
        Group branchNode = (Group) node;
        branchNode.readWriteLock.readLock().lock();
        try{
            branchNode.children.entrySet().stream()
                    // don't filter at group level. we cannot short circuit, else matching paths will never be reached.
                    // filters are fired at action levels. hence the only filter kept is PathMatcher.
                    // the full tree will be traversed always, else a sophisticated (depth first) tree traversal algorithm based on path pattern (?)
                    /*
                    .filter(e -> {
                        LOG.fine(String.format("testing predicate %s against node %s",event.predicate, e.getValue().path()));
                        return event.predicate.test(e.getValue());
                    })
                    */
                    .forEach(e -> {
                        if(e.getValue().type() == Node.Type.GROUP) {
                            AbstractNode branch = (AbstractNode) e.getValue();
                            branch.actorWrapper.tell(NodeActor.BranchEvent(event.payload, event.predicate), branchNode.actorWrapper);
                        }
                        else {
                            AbstractNode leaf = (AbstractNode) e.getValue();
                            leaf.actorWrapper.tell(NodeActor.LeafEvent(event.payload, event.predicate), branchNode.actorWrapper);
                        }
                    });
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            branchNode.readWriteLock.readLock().unlock();
        }
    }
    private void stop(){
        LOG.fine("Stopping actor on: "+node.path());
        if(node.type() == Node.Type.GROUP){
            Group branchNode = (Group) node;
            branchNode.readWriteLock.readLock().lock();
            try{
                branchNode.children.entrySet().stream()
                        .forEach(e -> {
                            AbstractNode value = (AbstractNode) e.getValue();
                            value.stopActor();
                        });
            }finally {
                branchNode.readWriteLock.readLock().unlock();
            }
        }
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
                .match(BranchEvent.class, event -> recurse(event))
                .match(LeafEvent.class, event -> {
                    if(event.predicate.test(node)) {
                        node.react(event.predicate, event.payload);
                    }
                })
                .match(StopEvent.class, event -> stop())
                .build();
    }
}
