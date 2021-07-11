package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.server.SimpleServer;

public class Bootstrap {
    public static void exit(){
        System.err.println("Usage: java -jar -Dserver.port=<mandatory> " +
                "-Ddb.file.path=<mandatory> -Dtemplate.config.dir=<optional> <app>.jar");
        System.exit(1);
    }
    public static void main(String[] args) {
        int port = Integer.parseInt( System.getProperty("server.port", "-1"));
        if(port == -1){
            exit();
        }

        new SimpleServer(port).start();
    }
}
