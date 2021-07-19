package org.reactiveminds.actiongraph.util;

import org.reactiveminds.actiongraph.react.templates.TemplateFunction;
import org.reactiveminds.actiongraph.util.err.TransientException;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface SystemProps {
    String DB_FILE_PATH = "db.file.path";
    String TEMPLATE_CONFIG_DIR = "template.config.dir";
    String SERVER_PORT = "server.port";
    String SERVER_PORT_DEFAULT = "4567";
    String SERVER_HANDLER = "server.handlers.max";
    String SERVER_HANDLER_DEFAULT = "10";
    String MAX_RETRY = "action.retry.count";
    String MAX_RETRY_DEFAULT = "3";
    String RETRY_BACKOFF = "action.retry.delay.backoff";
    String RETRY_BACKOFF_DEFAULT = "1.25";
    String RETRY_DELAY = "action.retry.delay";
    String RETRY_DELAY_DEFAULT = "3000";
    String STORE_PROVIDER = "store.provider.class";
    String DEFAULT_STORE_PROVIDER = "org.reactiveminds.actiongraph.store.mapdb.DefaultStoreProvider";
    String JOURNAL_EXPIRY = "action.journal.clean.sec";
    String JOURNAL_EXPIRY_DEFAULT = "180";
    String KAFKA_PROPS = "kafka.producer.properties";
    String KAFKA_PROPS_DEFAULT = "kafka.producer.properties";
    String KAFKA_SUPPLIER = "kafka.producer.supplier";
    String TEMPLATE_ENGINE = "template.engine";
    String TEMPLATE_ENGINE_DEFAULT = TemplateFunction.Engine.JavaScript.name();
    String MUTEX_BUCKETS = "store.mutex.buckets";
    String MUTEX_BUCKETS_DEFAULT = "64";
    String TEMPLATE_CONFIG_GROUP_PATTERN = "template.config.pattern.group";
    String TEMPLATE_CONFIG_ACTION_PATTERN = "template.config.pattern.action";

    List<Class<? extends Throwable>> TRANSIENT_ERRORS = Arrays.asList(TransientException.class, ConnectException.class);

}
