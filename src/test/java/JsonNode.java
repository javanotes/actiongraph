import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface JsonNode {
    ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
    String JSON_FUNC = "Java.asJSONCompatible(%s)";

    enum Type{Object, Array, Value, Missing}

    /**
     * Type of this {@link JsonNode}
     * @return
     */
    Type type();

    /**
     * Textual representation of this node
     * @return
     */
    String asText();

    /**
     * Accessor operation for {@link ObjectNode} types
     * @param key
     * @return
     */
    JsonNode get(String key);

    /**
     * Accessor operation for {@link ArrayNode} types
     * @param index
     * @return
     */
    JsonNode get(int index);

    /**
     * Read a json string
     * @param jsonBody
     * @return
     */
    static JsonNode fromText(String jsonBody){
        Object json = null;
        try {
            json = SCRIPT_ENGINE.eval(String.format(JSON_FUNC, jsonBody));
        }  catch (Exception e) {
            throw new UncheckedIOException(new IOException("json parse error", e));
        }
        return parse(json);
    }
    private static JsonNode parse(Object doc){
        return parseValue(doc);
    }
    private static JsonNode parseValue(Object o){
        if(o == null)
            return new MissingNode();
        if(o instanceof List){
            List<?> list = (List<?>) o;
            ArrayNode array = new ArrayNode();
            for(Object each: list){
                array.items.add(parseValue(each));
            }
            return array;
        }
        else if(o instanceof Map){
            Map<String, ?> map = (Map<String, ?>) o;
            ObjectNode obj = new ObjectNode();
            map.forEach((s,i) -> obj.entries.put(s, parseValue(i)));
            return obj;
        }
        else if(o instanceof Number){
            return new ValueNode<>((Number) o);
        }
        else
            return new ValueNode<>(o.toString());
    }
    class ObjectNode implements JsonNode{
        private final Map<String, JsonNode> entries = new HashMap<>();
        public void put(String key, JsonNode node){
            entries.put(key, node);
        }

        @Override
        public JsonNode.Type type() {
            return JsonNode.Type.Object;
        }

        @Override
        public String asText() {
            StringBuilder s = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, JsonNode> node: entries.entrySet()){
                if(first){
                    s.append("\"").append(node.getKey()).append("\"")
                            .append(":").append(node.getValue().asText());
                    first = false;
                }
                else {
                    s.append(",").append("\"").append(node.getKey()).append("\"")
                            .append(":").append(node.getValue().asText());
                }
            }
            s.append("}");
            return s.toString();
        }

        @Override
        public JsonNode get(String key) {
            return entries.get(key);
        }

        @Override
        public JsonNode get(int index) {
            return null;
        }
    }
    class ArrayNode implements JsonNode{
        private final List<JsonNode> items = new ArrayList<>();
        public void add(JsonNode node){
            items.add(node);
        }

        @Override
        public JsonNode.Type type() {
            return JsonNode.Type.Array;
        }

        @Override
        public String asText() {
            StringBuilder s = new StringBuilder("[");
            boolean first = true;
            for (JsonNode node: items){
                if(first){
                    s.append(node.asText());
                    first = false;
                }
                else {
                    s.append(",").append(node.asText());
                }
            }
            s.append("]");
            return s.toString();
        }

        @Override
        public JsonNode get(String key) {
            return null;
        }

        @Override
        public JsonNode get(int index) {
            return items.get(index);
        }
    }
    class ValueNode<T> implements JsonNode{
        public final T value;
        public ValueNode(T value) {
            this.value = value;
        }

        @Override
        public JsonNode.Type type() {
            return JsonNode.Type.Value;
        }

        @Override
        public String asText() {
            return value instanceof String ? "\""+value+"\"" : String.valueOf(value);
        }

        public T getValue() {
            return value;
        }

        @Override
        public JsonNode get(String key) {
            return null;
        }

        @Override
        public JsonNode get(int index) {
            return null;
        }
    }
    class MissingNode implements JsonNode{
        @Override
        public JsonNode.Type type() {
            return JsonNode.Type.Missing;
        }

        @Override
        public String asText() {
            return "null";
        }

        @Override
        public JsonNode get(String key) {
            return null;
        }

        @Override
        public JsonNode get(int index) {
            return null;
        }
    }
}
