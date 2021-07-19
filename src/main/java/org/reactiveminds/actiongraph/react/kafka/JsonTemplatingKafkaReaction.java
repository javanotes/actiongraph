package org.reactiveminds.actiongraph.react.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.reactiveminds.actiongraph.react.AbstractTemplateBasedReaction;
import org.reactiveminds.actiongraph.react.templates.TemplateFunction;
import org.reactiveminds.actiongraph.server.UrlPattern;
import org.reactiveminds.actiongraph.util.*;
import org.reactiveminds.actiongraph.util.err.ActionGraphException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class JsonTemplatingKafkaReaction extends AbstractTemplateBasedReaction {
    public static final String KAFKA_TRANSPORT = "kafka://{topic}:{key}";
    private static UrlPattern urlPattern = new UrlPattern(KAFKA_TRANSPORT);

    public JsonTemplatingKafkaReaction(String url, String actionPath, String template, TemplateFunction.Engine engine) {
        super(url, actionPath, template, engine);
        setup();
    }

    public static boolean endpointMatches(String endpoint){
        return urlPattern.matches(endpoint);
    }
    private Supplier<KafkaProducer<String, String>> producerFactory;
    /**
     * @param url
     * @param actionPath
     * @param jsonTemplate
     */

    private String topicName;
    private String keyProperty;
    private void configure(){
        final Properties config = new Properties();
        Map<String, String> map = urlPattern.match(endpoint);
        topicName = map.get("topic");
        keyProperty = map.get("key");
        Assert.notNull(topicName, "'topic' missing in Kafka endpoint");
        Assert.notNull(topicName, "'key property' missing in Kafka endpoint");
        String propFile = System.getProperty(SystemProps.KAFKA_PROPS, SystemProps.KAFKA_PROPS_DEFAULT);
        try {
            config.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(propFile));
        } catch (Exception e) {
            throw new ActionGraphException(String.format("Cannot load config file '%s' : %s", propFile, e.getMessage()), e);
        }
        try(AdminClient admin = AdminClient.create(config)){
            admin.listTopics().names().get();
        } catch (ExecutionException e) {
            throw new ActionGraphException(String.format("Unable to connect to Kafka using config '%s' : %s", propFile, Utils.primaryCause(e).getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        producerFactory = () -> new KafkaProducer<>(config);
    }
    private void setup(){
        Assert.isTrue(urlPattern.matches(endpoint), "Not a valid Kafka url! "+ endpoint);
        String supplier = System.getProperty(SystemProps.KAFKA_SUPPLIER);
        if(supplier != null){
            try {
                producerFactory = (Supplier<KafkaProducer<String, String>>) Class.forName(supplier).getConstructor().newInstance();
            } catch (Exception e) {
                throw new ActionGraphException("Unable to initialize producer supplier", e);
            }
        }
        else{
            configure();
        }

    }

    @Override
    public void destroy() {

    }

    @Override
    protected String content(String event, Map<String, String> headers) throws Exception {
        return templateFunction.apply(event);
    }

    @Override
    public void accept(String actionPath, String event) {
        try {
            final Map<String, String> headers = new HashMap<>();
            String content = content(event, headers);
            JsonNode node = ScriptUtil.evaluateJson(content);
            JsonNode key = node.get(keyProperty);
            try(Producer<String, String> producer = producerFactory.get()){
                producer.send(new ProducerRecord<>(topicName, key.asText(), content)).get();
            }

        }
        catch (Exception e) {
            onIOError(event, e);
        }
    }
}
