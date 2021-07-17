package org.reactiveminds.actiongraph.server;

public abstract class PostHttpService implements HttpService{
    @Override
    public Response doGet(Request request) {
        throw new UnsupportedOperationException("GET");
    }

    @Override
    public String method() {
        return "POST";
    }
}
