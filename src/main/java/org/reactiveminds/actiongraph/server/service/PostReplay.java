package org.reactiveminds.actiongraph.server.service;

import org.reactiveminds.actiongraph.core.ActionGraphException;
import org.reactiveminds.actiongraph.core.ActionGraphService;
import org.reactiveminds.actiongraph.server.HttpService;
import org.reactiveminds.actiongraph.server.PostHttpService;
import org.reactiveminds.actiongraph.store.ActionEntry;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.reactiveminds.actiongraph.util.Assert;
import org.reactiveminds.actiongraph.util.JSEngine;
import org.reactiveminds.actiongraph.util.JsonNode;

import javax.script.ScriptException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostReplay extends PostHttpService {

    @Override
    public Response doPost(Request request) {
        Response response = new Response();
        try {
            String corrId = request.getPathParams().get("corrId");
            Assert.notNull(corrId);
            ActionEntry entry = GraphStore.getEventJournal().getEntry(corrId);
            if(entry != null){
                boolean fire = ActionGraphService.fire(entry.getCorrelationId(), entry.getRoot(), entry.getPathMatcher(), entry.getPayload());
                if(fire) {
                    response.setStatusCode(HttpURLConnection.HTTP_ACCEPTED);
                    response.setContentType("text/plain");
                    response.setContent(entry.getCorrelationId());
                }
                else
                    response.setStatusCode(HttpURLConnection.HTTP_NOT_ACCEPTABLE);
            }
            else{
                response.setStatusCode(HttpURLConnection.HTTP_NOT_FOUND);
            }

        }
        catch (IllegalArgumentException | ActionGraphException e) {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
        }
        return response;
    }

    @Override
    public String pathPattern() {
        return "/actiongraph/replay/{corrId}";
    }
}
