package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.store.ActionData;
import org.reactiveminds.actiongraph.util.JSEngine;
import org.reactiveminds.actiongraph.util.JsonNode;
import org.reactiveminds.actiongraph.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;
import spark.utils.Assert;
import spark.utils.StringUtils;

import javax.script.ScriptException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

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
        Timer timer = new Timer();
        timer.start();
        ActionGraph.instance();
        String property = System.getProperty("server.port");
        if(property != null){
            Spark.port(Integer.parseInt(property));
        }
        property = System.getProperty("server.maxThreads");
        if(property != null){
            Spark.threadPool(Integer.parseInt(property));
        }
        getAction();
        getGroup();
        fire();
        Spark.init();
        Spark.awaitInitialization();

        timer.stop();
        Runtime.getRuntime().addShutdownHook(new Thread("shutdownhook"){
            @Override
            public void run(){
                Server.this.stop();
            }
        });
        LOG.info("Server is ready (elapsed {} ms) ", timer.lastLap(TimeUnit.MILLISECONDS));
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
        Spark.get("/actiongraph/groups/:root", ((request, response) -> {
            try {
                String root = ServerHandler.describeRoot(request.params(":root"));
                response.status(HttpURLConnection.HTTP_OK);
                response.header("Content-Type", "text/plain");
                return root;
            }
            catch (IllegalArgumentException | ActionGraphException e) {
                response.status(HttpURLConnection.HTTP_BAD_REQUEST);
                return e.getMessage();
            }
        }));
    }
    void getAction(){
        Spark.get("/actiongraph/actions", ((request, response) -> {
            try {
                Assert.isTrue(request.queryParams().contains("path"), "request param 'path' is required");
                ActionData actionData = ServerHandler.getActionData(request.queryParams("path"));
                response.header("Content-Type", "application/json");
                response.status(HttpURLConnection.HTTP_OK);
                return actionData.asJson().asText();
            }
            catch (IllegalArgumentException | ActionGraphException e) {
                response.status(HttpURLConnection.HTTP_BAD_REQUEST);
                return e.getMessage();
            }
        }));
    }
    void fire(){
        Spark.post("/actiongraph/fire/:root", ((request, response) -> {
            try {
                Assert.isTrue(!StringUtils.isEmpty(request.body()), "post body missing");
                JsonNode node = JSEngine.evaluateJson(request.body());
                JsonNode value = node.get("pathMatcher");
                String path = value.type() == JsonNode.Type.Missing ? null : (String) ((JsonNode.ValueNode)value).getValue();
                value = node.get("payload");
                Assert.isTrue(value.type() == JsonNode.Type.Value, "missing field 'payload'");
                String event = (String) ((JsonNode.ValueNode)value).getValue();
                boolean fire = ServerHandler.fire(request.params(":root"), path, event);
                if(fire) {
                    response.status(HttpURLConnection.HTTP_ACCEPTED);
                    return "OK";
                }
                else
                    response.status(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
            }
            catch (IllegalArgumentException | ActionGraphException | ScriptException e) {
                response.status(HttpURLConnection.HTTP_BAD_REQUEST);
                return e.getMessage();
            }
            return "Not OK";
        }));
    }
}
