package org.reactiveminds.actiongraph.node;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.reactiveminds.actiongraph.ActionGraph;
import org.reactiveminds.actiongraph.react.JsonTemplatingPostReaction;
import org.reactiveminds.actiongraph.react.Predicates;
import org.reactiveminds.actiongraph.util.JSEngine;
import org.reactiveminds.actiongraph.util.JsonNode;
import spark.Spark;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;

@RunWith(BlockJUnit4ClassRunner.class)
public class IntegrationTestSuite {

    static final String JSON_TEMPLATE = "{\"event\": \"orders\", \"orderId\":  \"#{$.split('|')[0]}\" , \"timestamp\":  \"#{$.split('|',2)[1]}\" }";
    @BeforeClass
    public static void init(){
        ActionGraph.instance();
        Spark.init();
        Spark.awaitInitialization();
    }
    @AfterClass
    public static void destroy(){
        ActionGraph.instance().release();
        Spark.stop();
    }
    @Test
    public void testPrettyPrintJson(){
        JsonNode.ObjectNode objectNode = new JsonNode.ObjectNode();
        objectNode.put("oneLeaf", new JsonNode.ValueNode<>("ONE"));
        objectNode.put("oneLeafAgain", new JsonNode.ValueNode<>("AGAIN"));
        objectNode.put("array", new JsonNode.ArrayNode());
        JsonNode.ArrayNode array = (JsonNode.ArrayNode) objectNode.get("array");
        array.add(new JsonNode.ValueNode<>(1));
        array.add(new JsonNode.ValueNode<>(2));
        array.add(new JsonNode.ValueNode<>(3));
        objectNode.put("inner", new JsonNode.ObjectNode());
        JsonNode.ObjectNode inner = (JsonNode.ObjectNode) objectNode.get("inner");
        inner.put("isInnerArray", new JsonNode.ValueNode<>(true));
        inner.put("innerArray", array);
        System.out.println(JsonNode.prettyPrint(objectNode));
        System.out.println(objectNode.asText());
    }
    @Test
    public void testPostConsumeSuccess() throws InterruptedException {
        Spark.post("/action/receiver", (request, response) -> {
            try {
                JsonNode jsonNode = JSEngine.evaluateJson(request.body());
                System.out.println("post body: "+ jsonNode.asText());
                System.out.println(JsonNode.prettyPrint(jsonNode));
            } catch (Exception e) {
                e.printStackTrace();
            }
            response.status(201);
            return "{\"response\": \"OK\"}";
        });
        Group orders = ActionGraph.instance().createGroup("/orders");
        JsonTemplatingPostReaction reaction = new JsonTemplatingPostReaction("http://localhost:4567/action/receiver", "", JSON_TEMPLATE);
        orders.getAction("submitToGateway", true).addObserver(reaction);
        orders.print(new PrintWriter(new OutputStreamWriter(System.out)));
        orders.react(Predicates.MATCH_ALL, "001|"+new Date());
        orders.react(Predicates.MATCH_ALL, "002|"+new Date());
        Thread.sleep(1000);
        Assert.assertEquals(2, reaction.getSuccessCount());
        Assert.assertEquals(0, reaction.getFailureCount());
    }
}
