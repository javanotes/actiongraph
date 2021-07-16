package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.react.Matchers;
import org.reactiveminds.actiongraph.store.ActionData;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.reactiveminds.actiongraph.store.GroupData;
import org.reactiveminds.actiongraph.util.Assert;

public class ActionGraphService {
    /**
     * Add a JsonTemplatingPostReaction action template

     */
    public static void saveActionData(ActionData actionData){
        Assert.notNull(actionData);
        Assert.notNull(actionData.getActionPath(), "action path cannot be null");
        for (ActionData.Props props : actionData.getProps()) {
            GraphStore.saveActionData(props.getJsonTemplate(), props.getUrl(), actionData.getActionPath());
        }
    }
    /**
     * Build a new topology
     */
    public static void saveGroupData(GroupData groupData){
        Assert.notNull(groupData);
        for (String s : groupData.getGraphs()) {
            GraphStore.saveGroupData(s, groupData.getRoot());
        }
    }
    public static ActionData getActionData(String path){
        Assert.notNull(path);
        return GraphStore.actionExists(path) ? GraphStore.actionData(path) : new ActionData();
    }
    public static boolean fire(String root, String pathPattern, String event){
        Assert.notNull(root, "root is null");
        Assert.notNull(event, "event is null");
        Group group = ActionGraph.instance().getRoot(root);
        if(group != null){
            group.react(pathPattern == null ? Matchers.ALL : Matchers.REGEX(pathPattern), event);
            return true;
        }
        return false;
    }
    public static String describeRoot(String root){
        return ActionGraph.instance().describeRoot(root);
    }
}