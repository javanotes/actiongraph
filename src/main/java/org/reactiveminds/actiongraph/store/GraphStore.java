package org.reactiveminds.actiongraph.store;

import org.reactiveminds.actiongraph.core.*;
import org.reactiveminds.actiongraph.react.Reaction;
import org.reactiveminds.actiongraph.react.http.JsonTemplatingRestReaction;
import org.reactiveminds.actiongraph.util.JSEngine;
import org.reactiveminds.actiongraph.util.JsonNode;
import org.reactiveminds.actiongraph.util.SystemProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GraphStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStore.class);
    private static StoreProvider storeProvider;
    public static QueueStore getMailboxQueue(String name, int size){
        return storeProvider.getMailBox(name, size);
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
            try {
                storeProvider.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            isOpen = false;
            LOGGER.info("file store closed");
        }
    }
    private static void openProvider(){
        try {
            storeProvider = (StoreProvider) Class.forName(System.getProperty(SystemProps.STORE_PROVIDER, SystemProps.DEFAULT_STORE_PROVIDER)).getConstructor().newInstance();
        }  catch (Exception e) {
            throw new ActionGraphException("Unable to load a store provider class!", e);
        }
        LOGGER.info("Loading saved configurations ..");
        storeProvider.open();
        LOGGER.info("Linking configurations ..");
        readConfigs();
        loadGroups();
        loadActions();
        isOpen = true;
    }
    public static void open(){
        if(!isOpen){
            synchronized (GraphStore.class){
                if(!isOpen){
                    openProvider();
                }
            }
        }
    }

    private static void readConfigs() {
        String configPath = System.getProperty(SystemProps.TEMPLATE_CONFIG_DIR);
        if(configPath != null){
            File f = new File(configPath);
            if(f.exists() && f.isDirectory()){
                LOGGER.info("Walking config directory {}, for config file/s of pattern '{}' or '{}'", f, GRP_CONFIG_FILE_PATTERN, ACT_CONFIG_FILE_PATTERN);
                readConfigs(f);
                LOGGER.info("Walk complete");
            }
        }
    }


    private static void loadActions() {
        storeProvider.loadAllActions().forEach(actionData -> {
            LOGGER.debug("loading actions at {}", actionData.getActionPath());
            actionData.getProps().forEach(props -> {
                try {
                    Action action = buildAction(props.url, props.jsonTemplate, actionData.getActionPath());
                    addReaction(action, props.url, props.jsonTemplate);
                } catch (Exception e) {
                    LOGGER.error("* Failed to load action {} * Error => {}", actionData.getActionPath(), e.getMessage());
                    LOGGER.debug("", e);
                }
            });
        });

    }

    private static void loadGroups() {
        storeProvider.loadAllGroups().forEach(groupData -> {
            LOGGER.debug("loading groups at {}", groupData.getRoot());
            groupData.getGraphs().forEach(json -> {
                try {
                    buildGroup(json, groupData.getRoot());
                } catch (Exception e) {
                    LOGGER.error("* Failed to load group {} * Error => {}", groupData.getRoot(), e.getMessage());
                    LOGGER.debug("", e);
                }
            });
        });

    }

    public static boolean groupExists(String root){
        return storeProvider.groupExists(root);
    }
    public static boolean actionExists(String path){
        return storeProvider.actionExists(path);
    }

    private static synchronized void commitGroup(String asJson, String graphRoot){
        GroupData o = storeProvider.loadGroup(graphRoot);
        if(o == null)
            o = new GroupData();
        o.getGraphs().add(asJson);
        o.setRoot(graphRoot);
        storeProvider.save(graphRoot, o);
    }
    private static synchronized void commitAction(String url, String postTemplate, String actionPath){
        ActionData o = storeProvider.loadAction(actionPath);
        if(o == null)
            o = new ActionData();
        o.setActionPath(actionPath);
        ActionData.Props props = new ActionData.Props();
        props.url = url;
        props.jsonTemplate = postTemplate;
        o.getProps().remove(props);
        o.getProps().add(props);
        storeProvider.save(actionPath, o);
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
        return storeProvider.loadAction(path);
    }
}
