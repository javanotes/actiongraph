package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.http.Request;
import org.reactiveminds.actiongraph.http.Response;
import org.reactiveminds.actiongraph.http.RestResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class PostApiReaction implements Reaction {
    private static final Logger LOGGER = Logger.getLogger(PostApiReaction.class.getSimpleName());
    protected String url;

    public PostApiReaction(String url) {
        this.url = url;
    }

    @Override
    public void destroy() {

    }

    /**
     * request formation exception
     * @param event
     * @param cause
     */
    protected void onIOError(String event, Throwable cause){
        LOGGER.log(Level.SEVERE, String.format("[onIOError] POST request creation failed event: %s", event), cause);
    }

    /**
     * On REST api response
     * @param response
     */
    protected abstract void onResponse(RestResponse response);

    /**
     * POST body, with additional header info if needed
     * @param event
     * @param headers
     * @return
     */
    protected abstract String content(String event, Map<String, String> headers) throws Exception;
    @Override
    public void accept(String actionPath, String event) {
        try {
            final Map<String, String> headers = new HashMap<>();
            String content = content(event, headers);
            Request request = Request.open(url, Collections.emptyMap());
            headers.forEach((k,v) -> request.addHeader(k,v));
            Response response = request.doPost(content);
            onResponse(new RestResponse(actionPath, event, response));
        }
        catch (Exception e) {
            onIOError(event, e);
        }
    }
}
