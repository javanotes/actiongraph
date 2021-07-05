package org.reactiveminds.actiongraph.http;

public class RestResponse {
    private final String actionPath;
    private final String event;
    private Response response;
    public RestResponse(String actionPath, String event) {
        this.actionPath = actionPath;
        this.event = event;
    }

    public RestResponse(String actionPath, String event, Response response) {
        this(actionPath, event);
        this.response = response;
    }

    public String getActionPath() {
        return actionPath;
    }

    public String getEvent() {
        return event;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
