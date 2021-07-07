package org.reactiveminds.actiongraph.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import org.reactiveminds.actiongraph.node.AbstractNode;
import scala.compat.java8.FutureConverters;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Actors {
    private final boolean durable;
    private Actors(){
        durable = Boolean.valueOf(System.getProperty("durable.mailbox", "false"));
        actorSystem = ActorSystem.create("action-groups");
    }

    Serialization serialization(){
        return SerializationExtension.get(actorSystem);
    }
    public final ActorReference actorOf(String name, AbstractNode n){
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        ActorRef actorRef = durable ?
                actorSystem
                .actorOf(NodeActorDurable.create(n, atomicBoolean), name.replaceAll("/", "\\.")) :
                actorSystem
                .actorOf(NodeActor.create(n, atomicBoolean), name.replaceAll("/", "\\."));
        return new ActorReference(actorRef, atomicBoolean);
    }
    final ActorSystem actorSystem;
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
            FutureConverters.toJava(actorSystem.terminate()).toCompletableFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}
