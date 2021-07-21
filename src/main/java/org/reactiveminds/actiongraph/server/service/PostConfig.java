package org.reactiveminds.actiongraph.server.service;

import org.reactiveminds.actiongraph.core.ActionGraphService;
import org.reactiveminds.actiongraph.server.PostHttpService;
import org.reactiveminds.actiongraph.server.model.TopologyModel;
import org.reactiveminds.actiongraph.util.Assert;
import org.reactiveminds.actiongraph.util.JsonNode;
import org.reactiveminds.actiongraph.util.ScriptUtil;
import org.reactiveminds.actiongraph.util.err.ActionGraphException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.Map;

public class PostConfig extends PostHttpService {
    private static final Logger log = LoggerFactory.getLogger(PostConfig.class);
    @Override
    public Response doPost(Request request) {
        Response response = new Response();
        try {
            JsonNode json = ScriptUtil.evaluateJson(request.getContent());
            Assert.isTrue(json.type() == JsonNode.Type.Object, "expecting object, found "+json.type());
            Map<String, JsonNode> entries = ((JsonNode.ObjectNode)json).getEntries();
            Assert.isTrue(entries.size() == 1, String.format("expecting a root object node, found multiple %s", entries.keySet()));
            String root = entries.keySet().iterator().next();
            TopologyModel topologyModel = new TopologyModel(json.get(root), root, "/"+root);
            ActionGraphService.saveTopology(topologyModel);
            response.setStatusCode(HttpURLConnection.HTTP_CREATED);
        }
        catch (IllegalArgumentException | ActionGraphException e) {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            response.setContentType("text/plain");
            response.setContent(e.getMessage());
            log.error(e.getMessage(), e);
            log.debug("", e);
        }
        return response;
    }

    @Override
    public String pathPattern() {
        return "/actiongraph/groups";
    }
}
