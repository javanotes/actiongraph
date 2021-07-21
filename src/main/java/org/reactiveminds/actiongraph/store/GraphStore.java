package org.reactiveminds.actiongraph.store;

import org.reactiveminds.actiongraph.core.*;
import org.reactiveminds.actiongraph.react.LoggingTemplateBasedReaction;
import org.reactiveminds.actiongraph.react.Reaction;
import org.reactiveminds.actiongraph.react.http.JsonTemplatingRestReaction;
import org.reactiveminds.actiongraph.react.kafka.JsonTemplatingKafkaReaction;
import org.reactiveminds.actiongraph.react.templates.TemplateFunction;
import org.reactiveminds.actiongraph.util.*;
import org.reactiveminds.actiongraph.util.err.ActionGraphException;
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

    private static void saveConfig(Path file, Node.Type engine) throws IOException, ScriptException, ActionGraphException {
        String content = String.join("", Files.readAllLines(file));
        JsonNode jsonNode = ScriptUtil.evaluateJson(content);
        switch (engine){
            case ACTION:
                String url = (String) ((JsonNode.ValueNode) jsonNode.get(ActionData.FIELD_ENDPOINT)).getValue();
                String actionPath = (String) ((JsonNode.ValueNode) jsonNode.get(ActionData.FIELD_PATH)).getValue();
                String postTemplate = jsonNode.get(ActionData.FIELD_TEMPLATE).asText();
                JsonNode node = jsonNode.get(ActionData.FIELD_SCRIPT);
                String script = null;
                if(node != null && node.type() == JsonNode.Type.Value){
                    script = (String) ((JsonNode.ValueNode) node).getValue();
                }
                commitAction(url, postTemplate, actionPath, script);
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

    /**
     * @deprecated
     */
    private static void readConfigs() {
        String configPath = System.getProperty(SystemProps.TEMPLATE_CONFIG_DIR);
        if(configPath != null){
            File f = new File(configPath);
            if(f.exists() && f.isDirectory()){
                LOGGER.warn("The recommended way to upload config is via the REST api");
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
                    Action action = buildAction(actionData.getActionPath());
                    addReaction(TemplateFunction.Engine.valueOf(actionData.getScript()), action, props.url, props.jsonTemplate);
                } catch (Exception e) {
                    LOGGER.error("* Failed to load action {} to endpoint [{}] * Error => {}", actionData.getActionPath(), props.url, e.getMessage());
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
    private static synchronized void commitAction(String url, String postTemplate, String actionPath, String script){
        ActionData o = storeProvider.loadAction(actionPath);
        if(o == null)
            o = new ActionData();
        o.setActionPath(actionPath);
        if (script != null) {
            o.setScript(script);
        }
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
        node = ScriptUtil.evaluateJson(asJson);
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
        Action action = buildAction(actionPath);
        TemplateFunction.Engine engine = TemplateFunction.Engine.valueOf(System.getProperty(SystemProps.TEMPLATE_ENGINE, SystemProps.TEMPLATE_ENGINE_DEFAULT));
        Reaction reaction = addReaction(engine, action, url, postTemplate);
        try {
            commitAction(url, postTemplate, action.path(), engine.name());
        }
        catch (Exception e){
            action.removeObserver(reaction);
            throw new ActionGraphException("Error saving action template", e);
        }
    }
    private static Reaction addReaction(TemplateFunction.Engine engine, Action action, String url, String postTemplate){
        Reaction reaction = Utils.isEmpty(url) ? new LoggingTemplateBasedReaction(url, action.path(), postTemplate, engine) :
                JsonTemplatingKafkaReaction.endpointMatches(url) ? new JsonTemplatingKafkaReaction(url, action.path(), postTemplate, engine)
                :  new JsonTemplatingRestReaction(url, action.path(), postTemplate, engine);
        action.addObserver(reaction);
        return reaction;
    }
    private static Action buildAction(String actionPath){
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
            if(entry.getValue().type() == JsonNode.Type.Array ){
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
    public static EventJournal getEventJournal(){
        return storeProvider.getEventJournal();
    }
}
