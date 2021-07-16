package org.reactiveminds.actiongraph.react.http;

import org.reactiveminds.actiongraph.react.Reaction;
import org.reactiveminds.actiongraph.util.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Reaction} base class for Http POST REST endpoints
 */
public abstract class AbstractRestReaction implements Reaction {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRestReaction.class);
    protected String url;

    public AbstractRestReaction(String url) {
        this.url = url;
    }

    /**
     * request formation exception
     * @param event
     * @param cause
     */
    protected void onIOError(String event, Throwable cause){
        LOGGER.debug(String.format("POST request failed for payload '%s' ==> %s", event, cause.getMessage()));
        LOGGER.debug("", cause);
        throw new TransientException(cause);
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
            RequestBuilder request = RequestBuilder.open(url, Collections.emptyMap());
            headers.forEach((k,v) -> request.addHeader(k,v));
            Response response = request.doPost(content);
            onResponse(new RestResponse(actionPath, event, response));
        }
        catch (Exception e) {
            onIOError(event, e);
        }
    }
}
