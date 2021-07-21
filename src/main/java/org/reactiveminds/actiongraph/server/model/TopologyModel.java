package org.reactiveminds.actiongraph.server.model;

import org.reactiveminds.actiongraph.util.Assert;
import org.reactiveminds.actiongraph.util.JsonNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TopologyModel extends ActionModel {
    @Override
    public String toString() {
        return "Topology{" +
                "group='" + group + '\'' +
                ", nodes=" + nodes +
                '}';
    }

    public TopologyModel(){}
    private void link(JsonNode groupVal, String actionPath){
        Assert.isTrue(groupVal.type() == JsonNode.Type.Object || groupVal.type() == JsonNode.Type.Array, String.format("INVALID_STRUCT: expecting object or array node type at '%s'", this.group));
        if(groupVal.type() == JsonNode.Type.Array){
            JsonNode.ArrayNode array = (JsonNode.ArrayNode) groupVal;
            for (JsonNode jsonNode : array.getItems()) {
                Assert.isTrue(jsonNode.type() == JsonNode.Type.Object, "INVALID_STRUCT: expecting array items to be object");
                getNodes().add(new ActionModel((JsonNode.ObjectNode) jsonNode, actionPath));
            }
        }
        else {
            JsonNode.ObjectNode object = ((JsonNode.ObjectNode) groupVal);
            for (Map.Entry<String, JsonNode> entry : object.getEntries().entrySet()) {
                getNodes().add(new TopologyModel(entry.getValue(), entry.getKey(), actionPath + "/" + entry.getKey()));
            }

        }
    }
    public TopologyModel(JsonNode jsonNode, String group, String path){
        this.group = group;
        this.actionPath = path;
        link(jsonNode, actionPath);
    }
    String group;
    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public List<ActionModel> getNodes() {
        return nodes;
    }

    public void setNodes(List<ActionModel> nodes) {
        this.nodes = nodes;
    }

    List<ActionModel> nodes = new LinkedList<>();
}
