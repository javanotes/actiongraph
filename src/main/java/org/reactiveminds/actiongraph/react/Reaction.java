package org.reactiveminds.actiongraph.react;

import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * Every action should have a reaction (and repercussions, maybe). The event should be immutable,
 * hence string
 */
public interface Reaction extends BiConsumer<String, String>, Serializable {
    void destroy();
}
