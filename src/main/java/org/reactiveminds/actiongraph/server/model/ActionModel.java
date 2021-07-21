package org.reactiveminds.actiongraph.server.model;

import org.reactiveminds.actiongraph.util.JsonNode;

import java.io.Serializable;

public class ActionModel implements Serializable {
    public ActionModel(){}
    public ActionModel(JsonNode.ObjectNode objectNode, String group){
        this.actionPath = group + "/" + ((JsonNode.ValueNode<String>) objectNode.get("actionPath")).getValue();
        this.actionEndpoint = ((JsonNode.ValueNode<String>) objectNode.get("actionEndpoint")).getValue();
        this.actionTemplate = objectNode.get("actionTemplate").asText();
    }
    String actionPath;

    @Override
    public String toString() {
        return "{" +
                "actionPath='" + actionPath + '\'' +
                ", actionEndpoint='" + actionEndpoint + '\'' +
                ", actionTemplate='" + actionTemplate + '\'' +
                '}';
    }

    public String getActionPath() {
        return actionPath;
    }

    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }

    public String getActionEndpoint() {
        return actionEndpoint;
    }

    public void setActionEndpoint(String actionEndpoint) {
        this.actionEndpoint = actionEndpoint;
    }

    public String getActionTemplate() {
        return actionTemplate;
    }

    public void setActionTemplate(String actionTemplate) {
        this.actionTemplate = actionTemplate;
    }

    String actionEndpoint;
    String actionTemplate;
}
