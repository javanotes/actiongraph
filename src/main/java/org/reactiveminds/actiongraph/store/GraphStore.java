package org.reactiveminds.actiongraph.store;

import akka.dispatch.Envelope;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.ActionGraph;
import org.reactiveminds.actiongraph.ActionGraphException;
import org.reactiveminds.actiongraph.node.Action;
import org.reactiveminds.actiongraph.node.Group;
import org.reactiveminds.actiongraph.react.JsonTemplatingPostReaction;
import org.reactiveminds.actiongraph.react.Reaction;
import org.reactiveminds.actiongraph.util.JSEngine;
import org.reactiveminds.actiongraph.util.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class GraphStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStore.class);
    static final String GROUPS = "GROUPS";
    static final String ACTIONS = "ACTIONS";
    private static DB mapDB;
    private static HTreeMap<String, ActionData> actionDB;
    private static HTreeMap<String, GroupData> groupDB;
    public static BlockingQueue<Envelope> getMailboxQueue(String name, Serializer<Envelope> serializer, int size){
        String qName = "CQ_"+name;
        return mapDB.exists(qName) ? mapDB.getCircularQueue(qName) : mapDB.createCircularQueue(qName, serializer, size);
    }
    private static File createDB(){
        File file = new File(System.getProperty("db.file.path", "./db/graphs.dat"));
        mapDB = DBMaker.newFileDB(file)
                .closeOnJvmShutdown()
                .make();

        groupDB = mapDB.createHashMap(GROUPS)
                .keySerializer(Serializer.STRING)
                .valueSerializer(new GraphDataSer())
                .makeOrGet();

        actionDB = mapDB.createHashMap(ACTIONS)
                .keySerializer(Serializer.STRING)
                .valueSerializer(new ActionDataSer())
                .makeOrGet();
        return file;
    }
    private static volatile boolean isOpen;
    public static synchronized void close(){
        if(isOpen){
            if(!mapDB.isClosed()) {
                mapDB.commit();
                mapDB.close();
            }
            isOpen = false;
            LOGGER.info("file store closed");
        }
    }
    public static void open(){
        if(!isOpen){
            synchronized (GraphStore.class){
                if(!isOpen){
                    File db = createDB();
                    loadGroups();
                    loadActions();
                    isOpen = true;
                    LOGGER.info("File store opened at: {}", db.getAbsolutePath());
                }
            }
        }
    }

    private static void loadActions() {
        actionDB.forEach((path, actionData) -> {
            LOGGER.debug("loading actions at {}", path);
            actionData.getProps().forEach(props -> {
                Action action = buildAction(props.url, props.jsonTemplate, path);
                addReaction(action, props.url, props.jsonTemplate);
            });
        });
    }

    private static void loadGroups() {
        groupDB.forEach((root, groupData) -> {
            LOGGER.debug("loading groups at {}", root);
            groupData.getJsonContents().forEach(json -> {
                buildGroup(json, root);
            });
        });
    }


    public static GroupData groupData(String graphRoot){
        open();
        if(!groupDB.containsKey(graphRoot))
            groupDB.putIfAbsent(graphRoot, new GroupData());

        return groupDB.get(graphRoot);
    }
    public static ActionData actionData(String actionPath){
        open();
        if(!actionDB.containsKey(actionPath))
            actionDB.putIfAbsent(actionPath, new ActionData());

        return actionDB.get(actionPath);
    }
    private static synchronized void saveGroup(String asJson, String graphRoot){
        GroupData o = groupData(graphRoot);
        o.getJsonContents().add(asJson);
        mapDB.commit();
    }
    private static synchronized void saveAction(String url, String postTemplate, String actionPath){
        ActionData o = actionData(actionPath);
        ActionData.Props props = new ActionData.Props();
        props.url = url;
        props.jsonTemplate = postTemplate;
        o.getProps().remove(props);
        o.getProps().add(props);
        mapDB.commit();
    }
    /**
     * Build a new topology
     * @param asJson
     * @param graphRoot
     */
    public static void saveGroupData(String asJson, String graphRoot){
        open();
        Group group = buildGroup(asJson, graphRoot);
        try {
            saveGroup(asJson, graphRoot);
        }
        catch (Exception e){
            group.delete();
            throw new ActionGraphException("Error saving group config", e);
        }
    }
    private static Group buildGroup(String asJson, String graphRoot){
        JsonNode node;
        try {
            node = JSEngine.evaluateJson(asJson);
        } catch (ScriptException e) {
            throw new ActionGraphException("PARSE_ERR: unable to parse action graph ", e);
        }
        if(node.type() != JsonNode.Type.Object){
            throw new ActionGraphException("PARSE_ERR: action graph json should be an object");
        }
        JsonNode.ObjectNode root = (JsonNode.ObjectNode) node;
        Group group = ActionGraph.instance().root(graphRoot);
        createNodes(root, group);
        return group;
    }

    /**
     * Add a JsonTemplatingPostReaction action template
     * @param url
     * @param postTemplate
     * @param actionPath
     */
    public static void saveActionData(String url, String postTemplate, String actionPath){
        open();
        Action action = buildAction(url, postTemplate, actionPath);
        Reaction reaction = addReaction(action, url, postTemplate);
        try {
            saveAction(url, postTemplate, action.path());
        }
        catch (Exception e){
            action.removeObserver(reaction);
            throw new ActionGraphException("Error saving action template", e);
        }
    }
    private static Reaction addReaction(Action action, String url, String postTemplate){
        JsonTemplatingPostReaction reaction = new JsonTemplatingPostReaction(url, action.path(), postTemplate);
        action.addObserver(reaction);
        return reaction;
    }
    private static Action buildAction(String url, String postTemplate, String actionPath){
        String _actionPath = actionPath.trim();
        if(_actionPath.endsWith("/"))
            _actionPath = _actionPath.substring(0, _actionPath.length()-1);
        int index = _actionPath.lastIndexOf('/');
        if(index == -1)
            throw new ActionGraphException("Invalid action path");
        String parent = _actionPath.substring(0, index);
        String child = _actionPath.substring(index+1);
        return ActionGraph.instance().getAction(parent, child);
    }
    private static void createNodes(JsonNode.ObjectNode root, Group group){
        for (Map.Entry<String, JsonNode> entry: root.getEntries().entrySet()){
            if(entry.getValue().type() == JsonNode.Type.Array || entry.getValue().type() == JsonNode.Type.Missing){
                throw new ActionGraphException("PARSE_ERR: action graph json cannot contain arrays");
            }
            if(entry.getValue().type() == JsonNode.Type.Object){
                Group child = group.changeGroup(entry.getKey(), true);
                createNodes((JsonNode.ObjectNode) entry.getValue(), child);
            }
            else{
                group.getAction(entry.getKey(), true);
            }
        }
    }
}
