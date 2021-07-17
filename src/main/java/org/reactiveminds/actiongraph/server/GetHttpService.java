package org.reactiveminds.actiongraph.server;

public abstract class GetHttpService implements HttpService{

    @Override
    public Response doPost(Request request) {
        throw new UnsupportedOperationException("POST");
    }

    @Override
    public String method() {
        return "GET";
    }
}
