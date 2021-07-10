package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.core.Node;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Predicates {
    private Predicates(){}
    public static final Predicate<Node> MATCH_ALL = n -> true;
    public static Predicate<Node> PathMatcher(String regex){
        return new PathMatcher(regex);
    }

    public static class PathMatcher implements Predicate<Node>{
        public Pattern getPattern() {
            return pattern;
        }

        private final Pattern pattern;

        public PathMatcher(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        public boolean test(Node node) {
            return pattern.matcher(node.path()).matches();
        }
    }
}
