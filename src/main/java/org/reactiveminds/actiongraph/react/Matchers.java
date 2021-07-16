package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.core.Node;

import java.util.regex.Pattern;

public final class Matchers {
    private Matchers(){}
    public static final ActionMatcher ALL = REGEX(".*");
    public static ActionMatcher REGEX(String regex){
        return new RegexMatcher(regex);
    }

    static class RegexMatcher implements ActionMatcher{
        private final Pattern pattern;
        public RegexMatcher(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        public boolean test(Node node) {
            return pattern.matcher(node.path()).matches();
        }

        @Override
        public String pattern() {
            return pattern.pattern();
        }
    }

}
