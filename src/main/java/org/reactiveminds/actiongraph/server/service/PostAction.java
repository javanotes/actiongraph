package org.reactiveminds.actiongraph.server.service;

import org.reactiveminds.actiongraph.util.err.ActionGraphException;
import org.reactiveminds.actiongraph.core.ActionGraphService;
import org.reactiveminds.actiongraph.server.PostHttpService;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.reactiveminds.actiongraph.util.Assert;
import org.reactiveminds.actiongraph.util.ScriptUtil;
import org.reactiveminds.actiongraph.util.JsonNode;

import javax.script.ScriptException;
import java.net.HttpURLConnection;

public class PostAction extends PostHttpService {

    @Override
    public Response doPost(Request request) {
        Response response = new Response();
        try {
            Assert.notEmpty(request.getContent(), "post body missing");
            JsonNode node = ScriptUtil.evaluateJson(request.getContent());
            JsonNode value = node.get("actionPath");
            String path = value.type() == JsonNode.Type.Missing ? null : (String) ((JsonNode.ValueNode)value).getValue();
            value = node.get("payload");
            Assert.isTrue(value != null && value.type() != JsonNode.Type.Missing, "missing field 'payload'");
            String event = (value.type() == JsonNode.Type.Value) ? (String) ((JsonNode.ValueNode)value).getValue() : value.asText();
            String root = request.getPathParams().get("root");
            Assert.notNull(root, "root group is missing");
            String correlationId = GraphStore.getEventJournal().createEntry(root, path, event);
            boolean fire = ActionGraphService.fire(correlationId, root, path, event);
            if(fire) {
                response.setStatusCode(HttpURLConnection.HTTP_ACCEPTED);
                response.setContentType("text/plain");
                response.setContent(correlationId);
            }
            else {
                response.setStatusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
            }
        }
        catch (IllegalArgumentException | ActionGraphException e) {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            response.setContentType("text/plain");
            response.setContent(e.getMessage());
        }
        return response;
    }

    @Override
    public String pathPattern() {
        return "/actiongraph/fire/{root}";
    }
}
