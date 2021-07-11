package org.reactiveminds.actiongraph.core;

import java.io.Serializable;

public interface Node extends Serializable {
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
