package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.core.Group;

public interface Node {
    enum Type{ACTION, GROUP}
    /**
     *
     * @return
     */
    boolean delete();
    /**
     *
     * @return
     */
    Type type();
    /**
     *
     * @return
     */
    Group parent();
    /**
     *
     * @return
     */
    String name();

    /**
     *
     * @return
     */
    String path();
    /**
     *
     * @return
     */
    boolean exists();

}
