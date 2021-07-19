package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.core.actor.Command;
import org.reactiveminds.actiongraph.react.ActionMatcher;
import org.reactiveminds.actiongraph.util.err.ActionGraphException;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;

public class Group extends AbstractNode{
    static String DATEFORMAT = "yyyy/MM/dd HH:mm:ss";
    private SimpleDateFormat formatter = new SimpleDateFormat(DATEFORMAT);
    protected Group(Group parent, String name, ReadWriteLock readWriteLock) {
        super(parent, name, readWriteLock);
    }

    // The ordering imposed by a comparator c on a set of elements S is said to be consistent with equals
    // if and only if c.compare(e1, e2)==0 has the same boolean value as e1.equals(e2) for every e1 and e2 in S.
    // so cannot use sorted (skiplist) maps to mimic insertion order :(
    final Map<TimedString, Node> children = new ConcurrentHashMap<>();

    /**
     *
     * @return
     */
    public Collection<Node> list(){
        readWriteLock.readLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("Dir is deleted!");
            return Collections.unmodifiableCollection(children.values());
        }finally {
            readWriteLock.readLock().unlock();
        }
    }

    private void print(PrintWriter writer, int level){
        StringBuilder gap = new StringBuilder();
        for (int i = 0; i < level; i++) {
            gap.append("\t");
        }
        if(level == 0)
            writer.println(" " + path());
        else
            writer.println(gap.toString()+" - "+type() + " " + name());
        gap.append("\t");
        children.entrySet().stream()
                .sorted(Comparator.comparingLong(o -> o.getKey().getTime()))
                .map(Map.Entry::getValue).forEach(node -> {
                    if(node.type() == Type.GROUP) {
                        ((Group) node).print(writer, level + 1);
                    }
                    else {
                        writer.println(gap.toString() + " - " + node.type() + " " + node.name());
                    }
                });
    }
    /**
     *
     */
    public void print(PrintWriter writer){
        readWriteLock.readLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("Dir is deleted!");
            print(writer, 0);
            writer.flush();
        }
        finally {
            readWriteLock.readLock().unlock();
        }

    }
    private void createChild(String child, Type type){
        readWriteLock.writeLock().lock();
        try{
            if(isDeleted)
                throw new ActionGraphException("Dir is deleted!");
            children.put(new TimedString(child), type == Type.GROUP ? new Group(this, child, readWriteLock)
                     : new Action(this, child, readWriteLock));
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }
    private Node getChild(String child){
        readWriteLock.readLock().lock();
        try{
            if(isDeleted)
                throw new ActionGraphException("Dir is deleted!");
            TimedString key = new TimedString(child);
            if(children.containsKey(key)){
                return children.get(key);
            }
        }finally {
            readWriteLock.readLock().unlock();
        }
        return null;
    }

    /**
     * Create or get directory
     * @param child
     * @param create
     * @return the created dir
     */
    public Group changeGroup(String child, boolean create){
        Node dir = getChild(child);
        if(dir == null && create){
            createChild(child, Type.GROUP);
        }
        dir = getChild(child);
        if(dir == null)
            throw new ActionGraphException("Child directory not found: "+child);
        return (Group) dir;
    }

    /**
     * Create or get directory.
     * @param child
     * @param create
     * @return the current dir
     */
    public Group makeGroup(String child, boolean create){
        Node dir = getChild(child);
        if(dir == null && create){
            createChild(child, Type.GROUP);
        }
        dir = getChild(child);
        if(dir == null)
            throw new ActionGraphException("Child directory not found: "+child);
        return this;
    }

    /**
     * Create or get file
     * @param name
     * @param create
     * @return
     */
    public Action getAction(String name, boolean create){
        Node file = getChild(name);
        if(file == null && create){
            createChild(name, Type.ACTION);
        }
        file = getChild(name);
        if(file == null)
            throw new ActionGraphException("Child file not found: "+name);
        return (Action) file;
    }

    @Override
    public boolean delete() {
        readWriteLock.writeLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("Dir is deleted!");
            // unlinking while iterating - thus requiring a concurrent map
            children.values().forEach(Node::delete);
            children.clear();
            return super.delete();
        }
        finally {
            readWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Type type() {
        return Type.GROUP;
    }
    @Override
    public final boolean react(String correlationId, ActionMatcher filter, String signal) {
        actorReference.tell(Command.newCommand(correlationId, Command.GROUP, signal, filter));
        return true;
    }

    public void walk(Consumer<AbstractNode> visitor){
        readWriteLock.readLock().lock();
        try{
            children.entrySet().stream()
                    .forEach(e -> visitor.accept((AbstractNode) e.getValue()));
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            readWriteLock.readLock().unlock();
        }
    }
}
