package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.server.EmbeddedServer;

public class Bootstrap {
    public static void exit(){
        System.err.println("Usage: " +
                "java -jar -Dserver.port=<mandatory> " +
                "-Ddb.file.path=<mandatory> " +
                "-Dtemplate.config.dir=<optional> <app>.jar");
        System.exit(1);
    }
    public static void main(String[] args) {
        new EmbeddedServer().run();
    }
}
