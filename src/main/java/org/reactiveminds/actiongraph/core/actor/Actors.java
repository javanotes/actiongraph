package org.reactiveminds.actiongraph.core.actor;

import akka.actor.*;
import akka.routing.BalancingPool;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import org.reactiveminds.actiongraph.core.AbstractNode;
import scala.compat.java8.FutureConverters;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class Actors {
    private Actors(){
        actorSystem = ActorSystem.create("action-groups");
    }

    public ActorRefProvider serializationSystemProvider(){
        return serialization().system().provider();
    }
    Serialization serialization(){
        return SerializationExtension.get(actorSystem);
    }

    /**
     *
     * @param name
     * @param n
     * @return
     */
    public final ActorReference actorOf(String name, AbstractNode n){
        AtomicBoolean atomicBoolean = new AtomicBoolean();
        ActorRef actorRef = actorSystem
                .actorOf(NodeActor.create(n, atomicBoolean), name.replaceAll("/", "\\."));

        return new ActorReference(actorRef, atomicBoolean);
    }

    /**
     *
     * @param actorClass
     * @param args
     * @param <T>
     * @return
     */
    public <T extends AbstractActor> ActorRef actorOf(Class<T> actorClass, Object...args){
        return actorSystem.actorOf(Props.create(actorClass, args));
    }
    final SupervisorStrategy strategy =
            new OneForOneStrategy(
                    5,
                    Duration.ofMinutes(1),
                    Collections.singletonList(Exception.class));

    /**
     *
     * @param workerClass
     * @param poolSize
     * @param name
     * @param args
     * @param <T>
     * @return
     */
    public <T extends AbstractActor> ActorRef routerActor(Class<T> workerClass, int poolSize, String name, Object...args){
        return actorSystem.actorOf(new BalancingPool(poolSize)
                .withSupervisorStrategy(strategy)
                .props(Props.create(workerClass)), name);
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
