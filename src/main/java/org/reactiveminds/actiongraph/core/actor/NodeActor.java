package org.reactiveminds.actiongraph.core.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import org.reactiveminds.actiongraph.core.AbstractNode;
import org.reactiveminds.actiongraph.core.Group;
import org.reactiveminds.actiongraph.core.Node;
import org.reactiveminds.actiongraph.store.EventJournal;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.reactiveminds.actiongraph.util.Assert;
import org.reactiveminds.actiongraph.util.SystemProps;
import org.reactiveminds.actiongraph.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

class NodeActor extends AbstractActor {
    private static final Logger LOG = LoggerFactory.getLogger(NodeActor.class);
    protected final AbstractNode node;
    protected final AtomicBoolean running;
    /**
     *
     * @param node
     * @param running
     */
    NodeActor(AbstractNode node, AtomicBoolean running) {
        this.node = node;
        this.running = running;
        LOG.debug("actor instantiation at {}", node.path());
    }

    public static Props create(AbstractNode node, AtomicBoolean running) {
        return Props.create(NodeActor.class, node, running);
    }
    void fireAction(Command command){
        try {
            if(command.predicate.test(node)){
                if(node.react(command.correlationId, command.predicate, command.payload)){
                    GraphStore.getEventJournal().markSuccess(command.correlationId, node.path());
                }
            }
            BoundedPersistentMailbox.flush(getSelf());
        } catch (Exception e) {
            BoundedPersistentMailbox.flush(getSelf());
            handleException(command, e);
        }
    }
    public static final int MAX_RETRY = Integer.parseInt(System.getProperty(SystemProps.MAX_RETRY, SystemProps.MAX_RETRY_DEFAULT));
    public static final double RETRY_BACKOFF = Double.parseDouble(System.getProperty(SystemProps.RETRY_BACKOFF, SystemProps.RETRY_BACKOFF_DEFAULT));
    public static final long INITIAL_RETRY_DELAY = Long.parseLong(System.getProperty(SystemProps.RETRY_DELAY, SystemProps.RETRY_DELAY_DEFAULT));

    private void deadLetter(Command command, Throwable e){
        LOG.error("[{}] Event is being dropped => {}", node.path(), e.getMessage());
        LOG.debug("", e);
        //getContext().getSystem().deadLetters().tell(new Command.DeadLetter(command, node.path(), Utils.primaryCause(e).getMessage()), getSelf());
        GraphStore.getEventJournal().markFailed(command.correlationId, node.path(),"ACTION_ERR: " + Utils.primaryCause(e).getMessage().substring(0,30));
    }
    private void handleException(Command command, Exception e) {
        if(!Utils.isTransientError(e)){
            deadLetter(command, e);
            return;
        }
        Command.ReplayCommand replayEvent;
        if(command.type() == 0){
            // it is in child retry actor
            replayEvent = new Command.ReplayCommand((Command.ReplayCommand) command);
        }
        else {
            LOG.warn("[{}] Event will be backed off and retried on exception => {}", node.path(), Utils.primaryCause(e).getMessage());
            // initial retry. so it is in parent actor
            replayEvent = new Command.ReplayCommand(command, System.currentTimeMillis(), Duration.ofMillis(INITIAL_RETRY_DELAY), 0);
        }
        if(replayEvent.retryCount < MAX_RETRY) {
            LOG.info("resubmitting event {}}", replayEvent);
            replayActor.tell(replayEvent, getSelf());
        }
        else {
            Assert.isTrue(replayActor == null, "Internal error! message retry exhausted, but replayActor is not null");
            deadLetter(command, e);
        }
    }

    void walkTree(Command command){
        Group branchNode = (Group) node;
        branchNode.walk(node -> {
            // don't filter at group level. we cannot short circuit, else matching paths will never be reached.
            // filters will be matched at action levels. hence the only filter kept is PathMatcher.
            // the full tree will be traversed always, else a sophisticated (depth first) tree traversal algorithm based on path pattern (?)
            if(node.type() == Node.Type.GROUP) {
                node.getActorReference().tell(Command.newCommand(command.correlationId, Command.GROUP, command.payload, command.predicate));
            }
            else {
                node.getActorReference().tell(Command.newCommand(command.correlationId, Command.ACTION, command.payload, command.predicate));
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
    protected ActorRef  replayActor;
    @Override
    public void preStart() throws Exception {
        super.preStart();
        if(node.type() == Node.Type.ACTION)
            replayActor = getContext().actorOf(ReplayActor.create(node, running, MAX_RETRY), node.path().replaceAll("/", "\\.") + "-r"+MAX_RETRY);
        synchronized (running){
            running.set(true);
            running.notifyAll();
        }
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.GroupCommand.class, event -> walkTree(event))
                .match(Command.ActionCommand.class, event -> fireAction(event))
                .match(Command.StopCommand.class, event -> stopReactors())
                .build();
    }

}
