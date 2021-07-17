package org.reactiveminds.actiongraph.server;

public abstract class GetPostHttpService implements HttpService{

    @Override
    public String method() {
        return "GET/POST";
    }
}
