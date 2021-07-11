package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.core.Node;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public final class Matchers {
    private Matchers(){}
    public static final ActionMatcher ALL = PATH("/**");
    public static ActionMatcher REGEX(String regex){
        return new RegexMatcher(regex);
    }
    public static ActionMatcher PATH(String path){
        return new GlobMatcher(path);
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
    static class GlobMatcher implements ActionMatcher{
        private final PathMatcher pattern;
        public GlobMatcher(String pattern) {
            this.pattern = FileSystems.getDefault().getPathMatcher("glob:"+pattern);
        }

        @Override
        public boolean test(Node node) {
            return pattern.matches(Paths.get(node.path()));
        }

        @Override
        public String pattern() {
            return pattern.toString();
        }
    }
}
