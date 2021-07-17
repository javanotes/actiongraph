package org.reactiveminds.actiongraph.server.service;

import org.reactiveminds.actiongraph.core.ActionGraphException;
import org.reactiveminds.actiongraph.core.ActionGraphService;
import org.reactiveminds.actiongraph.server.GetHttpService;
import org.reactiveminds.actiongraph.server.HttpService;
import org.reactiveminds.actiongraph.store.ActionEntry;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.reactiveminds.actiongraph.util.Assert;
import org.reactiveminds.actiongraph.util.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GetJournal extends GetHttpService {
    private static final Logger log = LoggerFactory.getLogger(GetJournal.class);
    @Override
    public Response doGet(Request request) {
        Response response = new Response();
        try {
            String corrId = request.getPathParams().get("corrId");
            Assert.notNull(corrId);
            ActionEntry entry = GraphStore.getEventJournal().getEntry(corrId);
            if(entry != null){
                response.setContentType("application/json");
                response.setStatusCode(HttpURLConnection.HTTP_OK);
                JsonNode.ObjectNode objectNode = new JsonNode.ObjectNode();
                objectNode.put("correlationId", new JsonNode.ValueNode<>(entry.getCorrelationId()));
                objectNode.put("status", new JsonNode.ValueNode<>(entry.getStatus()));
                objectNode.put("firedAt", new JsonNode.ValueNode<>(new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss").format(new Date(entry.getCreated()))));
                objectNode.put("additionalInfo", new JsonNode.ValueNode<>(entry.getAddlInfo()));
                response.setContent(objectNode.asText());
            }
            else{
                response.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
            }
        }
        catch (IllegalArgumentException | ActionGraphException e) {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            log.error("exception running service", e);
        }
        return response;
    }

    @Override
    public String pathPattern() {
        return "/actiongraph/journal/{corrId}";
    }
}
