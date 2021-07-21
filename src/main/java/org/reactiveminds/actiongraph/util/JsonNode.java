package org.reactiveminds.actiongraph.util;

import javax.script.ScriptException;
import java.util.*;

public interface JsonNode {
    enum Type{Object, Array, Value, Missing}
    Type type();
    String asText();
    JsonNode get(String key);
    JsonNode get(int index);
    default String format(){
        return ScriptUtil.prettyJson(asText());
    }
    static JsonNode parse(Object doc){
        return parseValue(doc);
    }

    static JsonNode parseValue(Object o){
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
        public Map<String, JsonNode> getEntries() {
            return entries;
        }

        public JsonNode firstChild(){
            return entries.entrySet().iterator().next().getValue();
        }
        public boolean contains(String key){
            return entries.containsKey(key);
        }
        private final LinkedHashMap<String, JsonNode> entries = new LinkedHashMap<>();
        public void put(String key, JsonNode node){
            entries.put(key, node);
        }

        @Override
        public Type type() {
            return Type.Object;
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
            return entries.containsKey(key) ? entries.get(key) : new MissingNode();
        }

        @Override
        public JsonNode get(int index) {
            return null;
        }
    }
    class ArrayNode implements JsonNode{
        public List<JsonNode> getItems() {
            return items;
        }

        private final List<JsonNode> items = new ArrayList<>();
        public void add(JsonNode node){
            items.add(node);
        }

        @Override
        public Type type() {
            return Type.Array;
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
        public Type type() {
            return Type.Value;
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
        public Type type() {
            return Type.Missing;
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
