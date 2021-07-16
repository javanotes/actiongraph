package org.reactiveminds.actiongraph.store.mapdb;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.reactiveminds.actiongraph.Bootstrap;
import org.reactiveminds.actiongraph.store.*;
import org.reactiveminds.actiongraph.store.QueueStore;
import org.reactiveminds.actiongraph.store.StoreProvider;
import org.reactiveminds.actiongraph.util.SystemProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public class DefaultStoreProvider implements StoreProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStoreProvider.class);
    static final String GROUPS = "GROUPS";
    static final String ACTIONS = "ACTIONS";
    private DB mapDB;
    private HTreeMap<String, ActionData> actionDB;
    private HTreeMap<String, GroupData> groupDB;
    private File createDB(){
        String dbPath = System.getProperty(SystemProps.DB_FILE_PATH);
        if(dbPath == null)
            Bootstrap.exit();
        File file = new File(dbPath);
        mapDB = DBMaker.newFileDB(file)
                //.mmapFileEnableIfSupported()
                .make();

        groupDB = mapDB.createHashMap(GROUPS)
                .keySerializer(Serializer.STRING)
                .valueSerializer(new GroupDataSer())
                .makeOrGet();

        actionDB = mapDB.createHashMap(ACTIONS)
                .keySerializer(Serializer.STRING)
                .valueSerializer(new ActionDataSer())
                .makeOrGet();
        return file;
    }
    private File dbFile;
    @Override
    public void open() {
        dbFile = createDB();
        LOGGER.info("File store opened at: {}", dbFile.getAbsolutePath());
    }

    private QueueStore mapDBQueue(String name, int size){
        String qName = "CQ_"+name;
        if(mapDB.isClosed()){
            LOGGER.warn("* getMailboxQueue called when db is closed! If this is on shutdown, can be ignored");
            return new FileQueue(qName, new ArrayBlockingQueue<>(size), mapDB);
        }
        LOGGER.debug("getting qname: {}",qName);
        return mapDB.exists(qName) ? new FileQueue(qName, mapDB.getCircularQueue(qName), mapDB)
                : new FileQueue(qName, mapDB.createCircularQueue(qName, new EnvelopeSerializer(), size), mapDB);
    }
    private QueueStore tapeQueue(String name){
        return new TapeQueue(name + ".q", dbFile.getParentFile());
    }
    private List<QueueStore> queueStores = Collections.synchronizedList(new ArrayList<>());
    @Override
    public QueueStore getMailBox(String name, int size) {
        QueueStore queueStore = tapeQueue(name);
        queueStores.add(queueStore);
        return queueStore;
    }

    @Override
    public ActionData loadAction(String actionId) {
        return actionDB.get(actionId);
    }

    @Override
    public List<ActionData> loadAllActions() {
        return new ArrayList<>(actionDB.values());
    }

    @Override
    public GroupData loadGroup(String groupId) {
        return groupDB.get(groupId);
    }

    @Override
    public boolean groupExists(String root) {
        return groupDB.containsKey(root);
    }

    @Override
    public boolean actionExists(String path) {
        return actionDB.containsKey(path);
    }

    @Override
    public List<GroupData> loadAllGroups() {
        return new ArrayList<>(groupDB.values());
    }

    @Override
    public void save(String id, ActionData actionData) {
        actionDB.put(id, actionData);
        mapDB.commit();
    }

    @Override
    public void save(String id, GroupData groupData) {
        groupDB.put(id, groupData);
        mapDB.commit();
    }

    @Override
    public void close() throws IOException {
        queueStores.forEach(queueStore -> {
            try {
                queueStore.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if(!mapDB.isClosed()) {
            mapDB.commit();
            mapDB.close();
        }
    }
}
