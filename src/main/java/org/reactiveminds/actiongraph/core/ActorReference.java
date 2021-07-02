package org.reactiveminds.actiongraph.core;

import akka.actor.ActorRef;
import akka.pattern.AskableActorRef;
import akka.util.Timeout;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

class ActorReference {
    private final ActorRef actorRef;
    private final AskableActorRef askableActorRef;
    private final AtomicBoolean running;

    public ActorReference(ActorRef actorRef, AtomicBoolean running) {
        this.actorRef = actorRef;
        this.running = running;
        askableActorRef = new AskableActorRef(actorRef);
    }

    public void tell(Object event, ActorReference sender){
        actorRef.tell(event, sender == null ? ActorRef.noSender() : sender.actorRef);
    }
    public Future<Object> ask(Object event, ActorReference sender, Duration timeout){
        return askableActorRef.ask(event, Timeout.create(timeout), sender == null ? ActorRef.noSender() : sender.actorRef);
    }
    public boolean awaitTermination(Duration duration) throws InterruptedException {
        synchronized (running) {
            if (running.get())
                return false;
            running.wait(duration.toMillis());
        }
        return running.get();
    }
}
