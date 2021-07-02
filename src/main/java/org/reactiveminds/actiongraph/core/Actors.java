package org.reactiveminds.actiongraph.core;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import scala.compat.java8.FutureConverters;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Actors {
    private Actors(){
        actors = ActorSystem.create("action-groups");
    }
    public static class ActorWrapper{
        public final ActorRef actorRef;
        private final AtomicBoolean running;

        public ActorWrapper(ActorRef actorRef, AtomicBoolean running) {
            this.actorRef = actorRef;
            this.running = running;
        }
        public boolean awaitTermination(Duration duration) throws InterruptedException {
            synchronized (running){
                if(running.get())
                    return false;
                running.wait(duration.toMillis());
            }
            return running.get();
        }
    }
    final ActorWrapper actorOf(String name, AbstractNode n){
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        ActorRef actorRef = actors.actorOf(NodeActor.create(n, atomicBoolean), name.replaceAll("/", "\\."));
        return new ActorWrapper(actorRef, atomicBoolean);
    }
    private final ActorSystem actors;
    private static volatile Actors THIS = null;
    public static Actors instance(){
        if(THIS == null){
            synchronized (Actors.class){
                if(THIS == null){
                    THIS = new Actors();
                }
            }
        }
        return THIS;
    }
    public void shutdown(){
        try {
            FutureConverters.toJava(actors.terminate()).toCompletableFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
