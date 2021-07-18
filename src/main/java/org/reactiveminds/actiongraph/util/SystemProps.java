package org.reactiveminds.actiongraph.util;

import org.reactiveminds.actiongraph.react.templates.TemplateFunction;

public interface SystemProps {
    String DB_FILE_PATH = "db.file.path";
    String TEMPLATE_CONFIG_DIR = "template.config.dir";
    String SERVER_PORT = "server.port";
    String SERVER_PORT_DEFAULT = "4567";
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
    String TEMPLATE_ENGINE = "template.engine";
    String TEMPLATE_ENGINE_DEFAULT = TemplateFunction.Engine.JavaScript.name();
    String EVENT_LOG = "event.log.enabled";
    String EVENT_LOG_DEFAULT = "true";
}
