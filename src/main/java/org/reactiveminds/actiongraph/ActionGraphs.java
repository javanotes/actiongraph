package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.core.Action;
import org.reactiveminds.actiongraph.core.Actors;
import org.reactiveminds.actiongraph.core.Group;

import java.io.OutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * An action graph topology to support hierarchical concurrent event processing.
 */
public final class ActionGraphs {
    static class SysoutConsoleHandler extends ConsoleHandler{
        @Override
        protected synchronized void setOutputStream(OutputStream out) throws SecurityException {
            super.setOutputStream(System.out);
        }
    }
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        System.setProperty("handlers", "org.reactiveminds.memfs.ActionGroups.SysoutConsoleHandler");
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).setUseParentHandlers(false);
    }
    private static final Logger LOG = Logger.getLogger(ActionGraphs.class.getName());
    private ActionGraphs(){}
    private static ActionGraphs THIS = new ActionGraphs();
    private final ConcurrentHashMap<String, Root> mounts = new ConcurrentHashMap<>();

    private Group getRoot(String name){
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
        LOG.info("stopping actor subsystem ..");
        Actors.instance().shutdown();
        shutdownThread = new Thread(() -> mounts.values().forEach(root -> {
            LOG.info(String.format("release: Unlinking root %s", root.path()));
            root.delete();
            LOG.info(String.format("release: Deleted root %s", root.path()));
        }));
        try {
            shutdownThread.join(Duration.ofSeconds(30).toMillis());
        } catch (InterruptedException e) {
            shutdownThread.interrupt();
        }
        mounts.clear();
        isShutdown = true;
    }
    public static ActionGraphs instance(){
        return THIS;
    }
    public synchronized Group root(String name){
        if(isShutdown)
            throw new IllegalStateException("System has been released!");
        return getRoot(name);
    }

    /**
     * Create branch path, if not exists
     * @param path dir path
     * @return dir
     */
    public synchronized Group createGroup(String path){
        if(isShutdown)
            throw new ActionGraphException("System has been released!");
        return getPredecessor(path, true);
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
        Group predecessor = getPredecessor(dirPath, true);
        return predecessor.getAction(file, true);
    }
    private Group getPredecessor(String path, boolean fullPath){
        if(path == null || path.trim().isEmpty() || !path.startsWith("/"))
            throw new ActionGraphException("Invalid path: "+path);
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
                root = root.changeGroup(split[i], true);
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
                throw new IllegalStateException("Removing root node is not allowed");
            return super.delete();
        }
    }
}
