package org.reactiveminds.actiongraph.actor;

import akka.actor.ActorRef;
import akka.pattern.AskableActorRef;
import akka.util.Timeout;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class ActorReference {
    private final ActorRef actorRef;
    private final AskableActorRef askableActorRef;
    private final AtomicBoolean running;

    public ActorReference(ActorRef actorRef, AtomicBoolean running) {
        this.actorRef = actorRef;
        this.running = running;
        askableActorRef = new AskableActorRef(actorRef);
    }

    static void deadLetter(Object event){
        Actors.instance().actorSystem.deadLetters().tell(event, ActorRef.noSender());
    }
    public void tell(Object event){
        actorRef.tell(event, actorRef);
    }
    public Future<Object> ask(Object event, Duration timeout){
        return askableActorRef.ask(event, Timeout.create(timeout), actorRef);
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
