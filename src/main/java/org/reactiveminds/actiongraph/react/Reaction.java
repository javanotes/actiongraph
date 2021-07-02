package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.core.Action;

import java.io.Serializable;
import java.util.function.BiConsumer;

/**
 * Every action should have a reaction (and repercussions, maybe)
 */
public interface Reaction extends BiConsumer<Action, Serializable> {
    void destroy();
}
