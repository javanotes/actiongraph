package org.reactiveminds.actiongraph.server;

import akka.actor.*;
import akka.routing.ActorRefRoutee;
import akka.routing.Routee;
import akka.routing.Router;
import akka.routing.SmallestMailboxRoutingLogic;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.reactiveminds.actiongraph.core.actor.Actors;
import org.reactiveminds.actiongraph.util.SystemProps;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class RequestDispatcher implements HttpHandler, Closeable {
    private final ActorRef routerActor;
    RequestDispatcher(){
        routerActor = Actors.instance()
                .routerActor(RequestHandler.class,
                        Integer.parseInt(System.getProperty(SystemProps.SERVER_HANDLER, SystemProps.SERVER_HANDLER_DEFAULT)), "http-handler");
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        routerActor.tell(exchange, ActorRef.noSender());
    }

    @Override
    public void close()  {
        routerActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    /**
     * @Deprecated
     */
    public static class RequestRouter extends AbstractActor{
        private Router router;

        public RequestRouter(int maxWorker) {
            List<Routee> routees = new ArrayList<>(maxWorker);
            for (int i = 0; i < maxWorker; i++) {
                ActorRef r = getContext().actorOf(Props.create(RequestHandler.class));
                getContext().watch(r);
                routees.add(new ActorRefRoutee(r));
            }
            router = new Router(new SmallestMailboxRoutingLogic(), routees);
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(
                            HttpExchange.class,
                            message -> {
                                router.route(message, getSelf());
                            })
                    .match(
                            PoisonPill.class,
                            message -> {
                                getContext().stop(getSelf());
                            })
                    .match(
                            Terminated.class,
                            message -> {
                                router = router.removeRoutee(message.actor());
                                ActorRef r = getContext().actorOf(Props.create(RequestHandler.class));
                                getContext().watch(r);
                                router = router.addRoutee(new ActorRefRoutee(r));
                            })
                    .build();
        }
    }
}
