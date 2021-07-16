package org.reactiveminds.actiongraph.server;

import com.sun.net.httpserver.HttpServer;
import org.reactiveminds.actiongraph.core.ActionGraph;
import org.reactiveminds.actiongraph.server.service.GetAction;
import org.reactiveminds.actiongraph.server.service.GetGroup;
import org.reactiveminds.actiongraph.server.service.PostAction;
import org.reactiveminds.actiongraph.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class SimpleServer extends Thread implements AutoCloseable{
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleServer.class);
    private final int port;
    private HttpServer httpServer;
    private RequestDispatcher requestDispatcher;

    public SimpleServer(int port) {
        super("bootstrap");
        this.port = port;
    }

    private volatile boolean running;

    @Override
    public void run() {
        if(running)
            return;
        Timer timer = new Timer();
        timer.start();
        ActionGraph.instance();
        try {
            requestDispatcher = new RequestDispatcher();
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", requestDispatcher);
            addHandlers();
            httpServer.start();
            running = true;
            Runtime.getRuntime().addShutdownHook(new Thread("shutdown"){
                @Override
                public void run(){
                    SimpleServer.this.close();
                }
            });
            timer.stop();
            LOGGER.info("Server listening on port {} (elapsed {} ms)",port, timer.lastLap(TimeUnit.MILLISECONDS));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addHandlers() {
        ServiceRegistry.register(new GetGroup());
        ServiceRegistry.register(new GetAction());
        ServiceRegistry.register(new PostAction());
    }

    @Override
    public void close()  {
        if(!running)
            return;
        LOGGER.warn("shutdown sequence initiated ..");
        requestDispatcher.close();
        ActionGraph.instance().release();
        httpServer.stop(1);
        running = false;
    }
}
