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
import org.reactiveminds.actiongraph.node.Node;
import org.reactiveminds.actiongraph.react.http.JsonTemplatingRestReaction;
import org.reactiveminds.actiongraph.react.Reaction;
import org.reactiveminds.actiongraph.util.JSEngine;
import org.reactiveminds.actiongraph.util.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

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
                .valueSerializer(new GroupDataSer())
                .makeOrGet();

        actionDB = mapDB.createHashMap(ACTIONS)
                .keySerializer(Serializer.STRING)
                .valueSerializer(new ActionDataSer())
                .makeOrGet();

        String configPath = System.getProperty("template.config.dir");
        if(configPath != null){
            File f = new File(configPath);
            if(f.exists() && f.isDirectory()){
                LOGGER.info("Walking config directory {}, for config file/s of pattern '{}' or '{}'", f, GRP_CONFIG_FILE_PATTERN, ACT_CONFIG_FILE_PATTERN);
                readConfigs(f);
                LOGGER.info("Walk complete");
            }
        }
        return file;
    }

    private static String GRP_CONFIG_FILE_PATTERN = System.getProperty("template.config.pattern.group", "g-.*");
    private static String ACT_CONFIG_FILE_PATTERN = System.getProperty("template.config.pattern.action", "a-.*");

    private static void saveConfig(Path file, Node.Type type) throws IOException, ScriptException, ActionGraphException{
        String content = String.join("", Files.readAllLines(file));
        JsonNode jsonNode = JSEngine.evaluateJson(content);
        switch (type){
            case ACTION:
                String url = (String) ((JsonNode.ValueNode) jsonNode.get(ActionData.FIELD_ENDPOINT)).getValue();
                String actionPath = (String) ((JsonNode.ValueNode) jsonNode.get(ActionData.FIELD_PATH)).getValue();
                String postTemplate = jsonNode.get(ActionData.FIELD_TEMPLATE).asText();
                commitAction(url, postTemplate, actionPath);
                LOGGER.info("Saved action config {}", file);
                break;
            case GROUP:
                String root = (String) ((JsonNode.ValueNode) jsonNode.get(GroupData.FIELD_ROOT)).getValue();
                String graph = jsonNode.get(GroupData.FIELD_GRAPH).asText();
                commitGroup(graph, root);
                LOGGER.info("Saved group config [{}]", file);
                break;
        }
    }
    private static void readConfigs(File f) {
        List<Path> groupFiles = new ArrayList<>();
        List<Path> actionFiles = new ArrayList<>();
        try {
            Files.walkFileTree(f.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException
                {
                    super.visitFile(file, attrs);
                    if(file.toFile().getName().matches(GRP_CONFIG_FILE_PATTERN)){
                        groupFiles.add(file);
                    }
                    else if(file.toFile().getName().matches(ACT_CONFIG_FILE_PATTERN)){
                        actionFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            LOGGER.error("Exception while traversing config directory ", e);
        }
        // save groups then action
        groupFiles.forEach(path -> {
            try {
                saveConfig(path, Node.Type.GROUP);
            } catch (Exception e) {
                LOGGER.error("Unable to save config file "+path, e);
            }
        });
        actionFiles.forEach(path -> {
            try {
                saveConfig(path, Node.Type.ACTION);
            } catch (Exception e) {
                LOGGER.error("Unable to save config file "+path, e);
            }
        });
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
                    LOGGER.info("Loading saved configurations ..");
                    File db = createDB();
                    LOGGER.info("Linking configurations ..");
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
                try {
                    Action action = buildAction(props.url, props.jsonTemplate, path);
                    addReaction(action, props.url, props.jsonTemplate);
                } catch (Exception e) {
                    LOGGER.error("* Failed to load action {} * Error => {}", path, e.getMessage());
                    LOGGER.debug("", e);
                }
            });
        });
    }

    private static void loadGroups() {
        groupDB.forEach((root, groupData) -> {
            LOGGER.debug("loading groups at {}", root);
            groupData.getGraphs().forEach(json -> {
                try {
                    buildGroup(json, root);
                } catch (Exception e) {
                    LOGGER.error("* Failed to load group {} * Error => {}", root, e.getMessage());
                    LOGGER.debug("", e);
                }
            });
        });
    }

    public static boolean groupExists(String root){
        return groupDB.containsKey(root);
    }
    public static boolean actionExists(String path){
        return actionDB.containsKey(path);
    }

    private static synchronized void commitGroup(String asJson, String graphRoot){
        GroupData o = groupDB.get(graphRoot);
        if(o == null)
            o = new GroupData();
        o.getGraphs().add(asJson);
        o.setRoot(graphRoot);
        groupDB.put(graphRoot, o);
        mapDB.commit();
    }
    private static synchronized void commitAction(String url, String postTemplate, String actionPath){
        ActionData o = actionDB.get(actionPath);
        if(o == null)
            o = new ActionData();
        o.setActionPath(actionPath);
        ActionData.Props props = new ActionData.Props();
        props.url = url;
        props.jsonTemplate = postTemplate;
        o.getProps().remove(props);
        o.getProps().add(props);
        actionDB.put(actionPath, o);
        mapDB.commit();
    }
    /**
     * Build a new topology
     * @param asJson
     * @param graphRoot
     */
    public static void saveGroupData(String asJson, String graphRoot){
        Group group = buildGroup(asJson, graphRoot);
        try {
            commitGroup(asJson, graphRoot);
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
        Group group = ActionGraph.instance().getOrCreateRoot(graphRoot);
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
        Action action = buildAction(url, postTemplate, actionPath);
        Reaction reaction = addReaction(action, url, postTemplate);
        try {
            commitAction(url, postTemplate, action.path());
        }
        catch (Exception e){
            action.removeObserver(reaction);
            throw new ActionGraphException("Error saving action template", e);
        }
    }
    private static Reaction addReaction(Action action, String url, String postTemplate){
        JsonTemplatingRestReaction reaction = new JsonTemplatingRestReaction(url, action.path(), postTemplate);
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

    public static ActionData actionData(String path) {
        return actionDB.get(path);
    }
}
