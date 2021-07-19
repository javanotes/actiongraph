package org.reactiveminds.actiongraph.server.service;

import org.reactiveminds.actiongraph.core.ActionGraphService;
import org.reactiveminds.actiongraph.util.err.ActionGraphException;
import org.reactiveminds.actiongraph.server.GetHttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

public class GetGroup extends GetHttpService {
    private static final Logger log = LoggerFactory.getLogger(GetGroup.class);
    @Override
    public Response doGet(Request request) {
        Response response = new Response();
        try {
            String root = ActionGraphService.describeRoot(request.getPathParams().get("root"));
            response.setStatusCode(HttpURLConnection.HTTP_OK);
            response.setContentType("text/plain");
            response.setContent(root);
        }
        catch (IllegalArgumentException | ActionGraphException e) {
            response.setStatusCode(HttpURLConnection.HTTP_BAD_REQUEST);
            log.error("exception running service", e);
        }
        return response;
    }

    @Override
    public String pathPattern() {
        return "/actiongraph/groups/{root}";
    }
}
