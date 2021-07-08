package org.reactiveminds.actiongraph.store;

import org.reactiveminds.actiongraph.util.JsonNode;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ActionData implements Serializable {
    public static final String FIELD_PATH = "actionPath";
    public static final String FIELD_ENDPOINT = "actionEndpoint";
    public static final String FIELD_TEMPLATE = "actionTemplate";
    public Set<Props> getProps() {
        return props;
    }

    public String getActionPath() {
        return actionPath;
    }

    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }

    public JsonNode.ObjectNode asJson(){
        JsonNode.ObjectNode node = new JsonNode.ObjectNode();
        node.put("actionPath", actionPath == null ? new JsonNode.MissingNode() : new JsonNode.ValueNode<>(actionPath));
        JsonNode.ArrayNode array = new JsonNode.ArrayNode();
        getProps().forEach(p -> {
            JsonNode.ObjectNode each = new JsonNode.ObjectNode();
            each.put("url", new JsonNode.ValueNode<>(p.getUrl()));
            each.put("jsonTemplate", new JsonNode.ValueNode<>(p.getJsonTemplate()));
            array.add(each);
        });
        node.put("props", array);
        return node;
    }
    private String actionPath;
    public void setProps(Set<Props> props) {
        this.props = props;
    }

    private Set<Props> props = new HashSet<>();
    public static class Props{
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Props props = (Props) o;
            return Objects.equals(url, props.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getJsonTemplate() {
            return jsonTemplate;
        }

        public void setJsonTemplate(String jsonTemplate) {
            this.jsonTemplate = jsonTemplate;
        }

        String url;
        String jsonTemplate;
    }
}
