package org.reactiveminds.actiongraph;

import org.reactiveminds.actiongraph.core.Action;
import org.reactiveminds.actiongraph.core.Group;

import java.io.Serializable;
import java.util.function.Predicate;

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
    /**
     * Fire a reactive execution down this subtree. It is imperative to keep the signal immutable since the same message will be passed
     * to different threads. Note, the order of processing is not guaranteed, as is with multithreaded operation.<p></p>
     * The 'reactions' will be handled at {@link Action} level by {@link Action#addObserver(Class)}, by passing {@link org.reactiveminds.actiongraph.react.AbstractReaction} instances.
     * @see org.reactiveminds.actiongraph.react.AbstractReaction
     * @param filter filter children to fire upon
     * @param signal the event (possibly immutable)
     */
    void react(Predicate<Node> filter, Serializable signal);
}
