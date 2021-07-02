package org.reactiveminds.actiongraph.core;

import org.reactiveminds.actiongraph.ActionGraphException;
import org.reactiveminds.actiongraph.Node;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Predicate;

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

    private void print(int level){
        StringBuilder gap = new StringBuilder();
        for (int i = 0; i < level; i++) {
            gap.append("\t");
        }
        if(level == 0)
            System.out.println(" -" + " " + path());
        else
            System.out.println(gap.toString()+" - "+type() + " " + name());
        gap.append("\t");
        children.entrySet().stream()
                .sorted(Comparator.comparingLong(o -> o.getKey().getTime()))
                .map(Map.Entry::getValue).forEach(node -> {
                    if(node.type() == Type.GROUP) {
                        ((Group) node).print(level + 1);
                    }
                    else {
                        System.out.println(gap.toString() + " - " + node.type() + " " + node.name());
                    }
                });
    }
    /**
     *
     */
    public void print(){
        readWriteLock.readLock().lock();
        try {
            if(isDeleted)
                throw new ActionGraphException("Dir is deleted!");
            print(0);
        }
        finally {
            readWriteLock.readLock().unlock();
        }

    }
    private void createChild(String child, Type type, boolean append){
        readWriteLock.writeLock().lock();
        try{
            if(isDeleted)
                throw new ActionGraphException("Dir is deleted!");
            children.put(new TimedString(child), type == Type.GROUP ? new Group(this, child, readWriteLock)
                    : append ? new AppendFileNode(this, child, readWriteLock) : new Action(this, child, readWriteLock));
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
    private static void checkName(String child){
        if(child == null || child.trim().isEmpty() || child.contains("/"))
            throw new IllegalArgumentException("Invalid child name '"+child+"'");
    }

    /**
     * Create or get directory
     * @param child
     * @param create
     * @return the created dir
     */
    public Group changeGroup(String child, boolean create){
        checkName(child);
        Node dir = getChild(child);
        if(dir == null && create){
            createChild(child, Type.GROUP, false);
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
        checkName(child);
        Node dir = getChild(child);
        if(dir == null && create){
            createChild(child, Type.GROUP, false);
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
        return getAction(name, create, false);
    }

    /**
     * Create or get file in append mode
     * @param name
     * @param create
     * @param append
     * @return
     */
    public Action getAction(String name, boolean create, boolean append){
        checkName(name);
        Node file = getChild(name);
        if(file == null && create){
            createChild(name, Type.ACTION, append);
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
            children.values().forEach(node -> node.delete());
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


    public final void react(Predicate<Node> filter, Serializable signal) {
        actorWrapper.tell(NodeActor.BranchEvent(signal, filter), null);
    }

}
