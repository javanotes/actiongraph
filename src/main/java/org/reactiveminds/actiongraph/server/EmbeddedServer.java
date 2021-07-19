package org.reactiveminds.actiongraph.server;

import org.reactiveminds.actiongraph.util.SystemProps;

import java.time.Duration;

/**
 * A utility class to run the server in-process.
 */
public class EmbeddedServer {
    public EmbeddedServer listenerPort(int port){
        System.setProperty(SystemProps.SERVER_PORT, ""+port);
        return this;
    }
    public EmbeddedServer maxHandlerConcurrency(int concurrency){
        System.setProperty(SystemProps.SERVER_HANDLER, ""+concurrency);
        return this;
    }
    public EmbeddedServer templateConfigDir(String configDir){
        System.setProperty(SystemProps.TEMPLATE_CONFIG_DIR, ""+configDir);
        return this;
    }
    public EmbeddedServer dbStorePath(String configDir){
        System.setProperty(SystemProps.DB_FILE_PATH, ""+configDir);
        return this;
    }
    public EmbeddedServer maxRetries(int retry){
        System.setProperty(SystemProps.MAX_RETRY, ""+retry);
        return this;
    }
    public EmbeddedServer initialRetryDelay(Duration delay){
        System.setProperty(SystemProps.RETRY_DELAY, ""+delay.toMillis());
        return this;
    }
    public EmbeddedServer retryBackoffFactor(double backoff){
        System.setProperty(SystemProps.RETRY_BACKOFF, ""+backoff);
        return this;
    }
    public EmbeddedServer kafkaProducerSupplier(String supplierClass){
        System.setProperty(SystemProps.KAFKA_SUPPLIER, supplierClass);
        return this;
    }
    public EmbeddedServer storeProviderImpl(String providerClass){
        System.setProperty(SystemProps.STORE_PROVIDER, providerClass);
        return this;
    }
    public EmbeddedServer templatePatternGroup(String pattern){
        System.setProperty(SystemProps.TEMPLATE_CONFIG_GROUP_PATTERN, pattern);
        return this;
    }
    public EmbeddedServer templatePatternAction(String pattern){
        System.setProperty(SystemProps.TEMPLATE_CONFIG_ACTION_PATTERN, pattern);
        return this;
    }

    /**
     * Start the server on a dedicated thread
     * @return
     */
    public AutoCloseable run(){
        SimpleServer server = new SimpleServer();
        server.start();
        return server;
    }
}
