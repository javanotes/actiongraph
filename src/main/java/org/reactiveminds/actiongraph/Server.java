package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.util.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

public class Server implements Runnable{
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    @Override
    public void run() {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            System.err.println("-------------------------------------------------");
            System.err.println("*** UNEXPECTED ERROR ON START *** exiting process");
            System.err.println("-------------------------------------------------");
            e.printStackTrace();
            System.exit(1);
        });
        LOG.info("Initiating action graph ..");
        LOG.info("Loading saved configurations ..");
        ActionGraph.instance();
        String property = System.getProperty("server.port");
        if(property != null){
            Spark.port(Integer.parseInt(property));
        }
        property = System.getProperty("server.maxThreads");
        if(property != null){
            Spark.threadPool(Integer.parseInt(property));
        }
        Spark.init();
        Spark.awaitInitialization();
        Runtime.getRuntime().addShutdownHook(new Thread("shutdownhook"){
            @Override
            public void run(){
                Server.this.stop();
            }
        });
        LOG.info("Server is ready ");
    }
    void stop(){
        LOG.warn("shutdown sequence initiated ..");
        ActionGraph.instance().release();
        Spark.stop();
    }

    public static void main(String[] args) {
        new Thread(new Server(), "bootstrap").start();
    }
    // APIs
    void getGroup(){
        Spark.get("/actiongraph/groups/{root}", ((request, response) -> {
            return new JsonNode.ObjectNode().asText();
        }));
    }
    void getAction(){
        Spark.get("/actiongraph/actions/{root}", ((request, response) -> {
            return new JsonNode.ObjectNode().asText();
        }));
    }
}
