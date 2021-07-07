package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.node.Action;
import org.reactiveminds.actiongraph.actor.Actors;
import org.reactiveminds.actiongraph.node.Group;
import org.reactiveminds.actiongraph.store.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An action graph topology to support hierarchical observers
 */
public final class ActionGraph {

    private static final Logger LOG = LoggerFactory.getLogger(ActionGraph.class);
    private ActionGraph(){
        GraphStore.open();
        Actors.instance();
    }
    private static ActionGraph THIS = new ActionGraph();
    private final ConcurrentHashMap<String, Root> mounts = new ConcurrentHashMap<>();

    private Group getRoot(String name){
        if(name == null || name.trim().isEmpty() || name.contains("/"))
            throw new ActionGraphException("Invalid root name - cannot be null/empty, cannot contain '/'");
        if(!mounts.containsKey(name)){
            mounts.putIfAbsent(name, new Root(null, name, new ReentrantReadWriteLock()));
        }
        return mounts.get(name);
    }
    private volatile boolean isShutdown;
    private Thread shutdownThread;
    public synchronized void release()  {
        if(isShutdown)
            return;
        shutdownThread = new Thread(() -> mounts.values().forEach(root -> {
            LOG.info(String.format("releasing root %s ..", root.path()));
            root.delete();
            LOG.warn(String.format("** released root %s **", root.path()));
        }));
        shutdownThread.start();
        try {
            shutdownThread.join(Duration.ofSeconds(30).toMillis());
        } catch (InterruptedException e) {
            shutdownThread.interrupt();
        }
        mounts.clear();
        LOG.info("stopping actor subsystem ..");
        Actors.instance().shutdown();
        GraphStore.close();
        isShutdown = true;
    }
    public static ActionGraph instance(){
        return THIS;
    }
    public synchronized Group root(String name){
        if(isShutdown)
            throw new ActionGraphException("System has been released!");
        return getRoot(name);
    }

    /**
     * Create or get group node at the given path
     * @param path dir path
     * @return dir
     */
    public synchronized Group createGroup(String path){
        if(isShutdown)
            throw new ActionGraphException("System has been released!");
        return getPredecessor(path, true, true);
    }

    /**
     * Create or get action node at given path
     * @param dirPath parent dir path
     * @param file file name
     * @return file
     */
    public synchronized Action createAction(String dirPath, String file){
        if(isShutdown)
            throw new ActionGraphException("System has been released!");
        Group predecessor = getPredecessor(dirPath, true, true);
        return predecessor.getAction(file, true);
    }
    public synchronized Action getAction(String dirPath, String file){
        if(isShutdown)
            throw new ActionGraphException("System has been released!");
        Group predecessor = getPredecessor(dirPath, true, false);
        return predecessor.getAction(file, false);
    }
    private Group getPredecessor(String path, boolean fullPath, boolean createIfNotExists){
        if(path == null || path.trim().isEmpty() || !path.startsWith("/"))
            throw new ActionGraphException("Invalid path (should start with \"/\"): "+path);
        String sanitized = path.trim();
        char[] chars = sanitized.toCharArray();
        while(chars[0] == '/'){
            chars = Arrays.copyOfRange(chars, 1, chars.length);
        }
        while (chars[chars.length-1] == '/'){
            chars = Arrays.copyOfRange(chars, 0, chars.length-1);
        }
        sanitized = String.copyValueOf(chars);
        String[] split = sanitized.split("/");
        Group root = root(split[0]);
        if(split.length > 1){
            for(int i=1; i< (fullPath ? split.length : split.length-1); i++){
                root = root.changeGroup(split[i], createIfNotExists);
            }
        }
        return root;
    }

    private class Root extends Group {
        Root(Group parent, String name, ReadWriteLock readWriteLock) {
            super(parent, name, readWriteLock);
        }
        @Override
        public boolean delete() {
            if(Thread.currentThread() != shutdownThread)
                throw new ActionGraphException("Removing root node is not allowed! This operation can only done on release()");
            return super.delete();
        }
    }
}
