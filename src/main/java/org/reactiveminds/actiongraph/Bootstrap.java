package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.server.SimpleServer;

public class Bootstrap {
    public static void main(String[] args) {
        int port = Integer.parseInt( System.getProperty("server.port", "-1"));
        if(port == -1){
            System.err.println("'-Dserver.port' not specified");
            System.exit(1);
        }

        new SimpleServer(port).start();
    }
}
