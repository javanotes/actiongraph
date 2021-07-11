package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.core.Node;

import java.util.function.Predicate;

public interface ActionMatcher extends Predicate<Node> {
    String pattern();
}
