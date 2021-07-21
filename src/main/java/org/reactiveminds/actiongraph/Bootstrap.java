package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.server.EmbeddedServer;

public class Bootstrap {
    public static void main(String[] args) {
        new EmbeddedServer().run();
    }
}
