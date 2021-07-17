package org.reactiveminds.actiongraph.store;

import java.io.Closeable;
import java.util.List;

/**
 * File store provider
 */
public interface StoreProvider extends Closeable {
    EventJournal getEventJournal();
    /**
     * Open the file backed store
     */
    void open();

    /**
     *
     * @param name
     * @param size
     * @return
     */
    QueueStore getMailBox(String name, int size);

    /**
     *
     * @param actionId
     * @return
     */
    ActionData loadAction(String actionId);

    /**
     *
     * @return
     */
    List<ActionData> loadAllActions();

    /**
     *
     * @param groupId
     * @return
     */
    GroupData loadGroup(String groupId);

    /**
     *
     * @param root
     * @return
     */
    boolean groupExists(String root);

    /**
     *
     * @param path
     * @return
     */
    boolean actionExists(String path);

    /**
     *
     * @return
     */
    List<GroupData> loadAllGroups();

    /**
     *
     * @param id
     * @param actionData
     */
    void save(String id, ActionData actionData);

    /**
     *
     * @param id
     * @param groupData
     */
    void save(String id, GroupData groupData);
}
