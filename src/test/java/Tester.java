import org.reactiveminds.actiongraph.react.kafka.JsonTemplatingKafkaReaction;
import org.reactiveminds.actiongraph.server.UrlPattern;

import java.util.Map;

public class Tester {
    public static void main(String[] args) {
        UrlPattern pattern = new UrlPattern(JsonTemplatingKafkaReaction.KAFKA_TRANSPORT);
        Map<String, String> match = pattern.match("kafka://topicName:keyProperty");
        System.out.println(match);
        boolean matches = pattern.matches("kafka://topicName:keyProperty");
        System.out.println(matches);
    }
}
