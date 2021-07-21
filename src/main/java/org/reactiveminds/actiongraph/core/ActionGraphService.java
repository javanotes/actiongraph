package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.react.Matchers;
import org.reactiveminds.actiongraph.react.templates.TemplateFunction;
import org.reactiveminds.actiongraph.server.model.ActionModel;
import org.reactiveminds.actiongraph.server.model.TopologyModel;
import org.reactiveminds.actiongraph.store.ActionData;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.reactiveminds.actiongraph.store.GroupData;
import org.reactiveminds.actiongraph.util.Assert;
import org.reactiveminds.actiongraph.util.JsonNode;

public class ActionGraphService {
    private static void childGroup(ActionModel actionModel, JsonNode.ObjectNode graph){
        if(actionModel instanceof TopologyModel){
            JsonNode.ObjectNode childGraph = new JsonNode.ObjectNode();
            ((TopologyModel) actionModel).getNodes().forEach(actionModel1 -> childGroup(actionModel1, childGraph));
            graph.put(((TopologyModel) actionModel).getGroup(), childGraph);
        }
        else{
            int indexOf = actionModel.getActionPath().lastIndexOf('/');
            graph.put(actionModel.getActionPath().substring(++indexOf), new JsonNode.ValueNode<>(""));
        }
    }
    private static void childAction(ActionModel actionModel){
        if(actionModel instanceof TopologyModel){
            ((TopologyModel) actionModel).getNodes()
                    .forEach(actionModel1 -> childAction( actionModel1));
        }
        else{
            ActionData actionData = new ActionData();
            actionData.setScript(TemplateFunction.Engine.JsonPath.name());
            actionData.setActionPath(actionModel.getActionPath());
            actionData.getProps().add(new ActionData.Props(actionModel.getActionEndpoint(), actionModel.getActionTemplate()));
            saveActionData(actionData);
        }
    }
    public static void saveTopology(TopologyModel model){
        GroupData groupData = new GroupData();
        groupData.setRoot(model.getGroup());
        JsonNode.ObjectNode graph = new JsonNode.ObjectNode();
        childGroup(model, graph);
        groupData.getGraphs().add(graph.firstChild().asText());
        saveGroupData(groupData);
        childAction(model);
    }
    /**
     * Add a JsonTemplatingPostReaction action template

     */
    static void saveActionData(ActionData actionData){
        Assert.notNull(actionData);
        Assert.notNull(actionData.getActionPath(), "action path cannot be null");
        for (ActionData.Props props : actionData.getProps()) {
            GraphStore.saveActionData(props.getJsonTemplate(), props.getUrl(), actionData.getActionPath());
        }
    }
    /**
     * Build a new topology
     */
    static void saveGroupData(GroupData groupData){
        Assert.notNull(groupData);
        for (String s : groupData.getGraphs()) {
            GraphStore.saveGroupData(s, groupData.getRoot());
        }
    }
    public static ActionData getActionData(String path){
        Assert.notNull(path);
        return GraphStore.actionExists(path) ? GraphStore.actionData(path) : new ActionData();
    }

    /**
     * Trigger an action graph to fire event/s
     * @param correlationId
     * @param root
     * @param pathPattern
     * @param eventPayload
     * @return
     */
    public static boolean fire(String correlationId, String root, String pathPattern, String eventPayload){
        Assert.notNull(root, "root is null");
        Assert.notNull(eventPayload, "event is null");
        Group group = ActionGraph.instance().getRoot(root);
        if(group != null){
            group.react(correlationId, pathPattern == null || pathPattern.equalsIgnoreCase("all") ? Matchers.ALL : Matchers.REGEX(pathPattern), eventPayload);
            return true;
        }
        return false;
    }
    public static String describeRoot(String root){
        return ActionGraph.instance().describeRoot(root);
    }
}
