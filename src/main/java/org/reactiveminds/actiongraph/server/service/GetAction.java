package org.reactiveminds.actiongraph.server.service;

import org.reactiveminds.actiongraph.core.ActionGraphException;
import org.reactiveminds.actiongraph.core.ActionGraphService;
import org.reactiveminds.actiongraph.server.GetHttpService;
import org.reactiveminds.actiongraph.server.HttpService;
import org.reactiveminds.actiongraph.store.ActionData;
import org.reactiveminds.actiongraph.util.Assert;

import java.net.HttpURLConnection;

public class GetAction extends GetHttpService {
    @Override
    public Response doGet(Request request) {
        Response response = new Response();
        try {
            Assert.isTrue(request.getQueryParams().containsKey("path"), "request param 'path' is required");
            ActionData actionData = ActionGraphService.getActionData(request.getQueryParams().get("path"));
            response.setContentType("application/json");
            response.setStatusCode(HttpURLConnection.HTTP_OK);
            response.setContent( actionData.asJson().asText());
        }
        catch (IllegalArgumentException | ActionGraphException e) {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            response.setContent( e.getMessage());
        }
        return response;
    }
    @Override
    public String pathPattern() {
        return "/actiongraph/actions";
    }
}
