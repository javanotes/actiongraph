package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Predicates {
    private Predicates(){}
    public static final Predicate<Node> MATCH_ALL = n -> true;
    public static class CommonPredecessorFilter implements Predicate<Node>{
        final List<String> predecessors = new ArrayList<>();

        public CommonPredecessorFilter(List<String> predecessors) {
            this.predecessors.addAll(predecessors);
        }

        @Override
        public boolean test(Node node) {
            return predecessors.stream().filter(s -> node.path().startsWith(s)).findFirst().isPresent();
        }
    }
    public static class CommonPredecessorAndGroupsInFilter extends CommonPredecessorFilter{
        private final List<String> nodeNames;
        public CommonPredecessorAndGroupsInFilter(List<String> predecessors, List<String> nodeNames) {
            super(predecessors);
            this.nodeNames = nodeNames;
        }

        @Override
        public boolean test(Node node) {
            return super.test(node) && nodeNames.contains(node.name());
        }
    }
    public static class CommonPredecessorAndActionsInFilter extends CommonPredecessorFilter{
        private final List<String> nodeNames;
        public CommonPredecessorAndActionsInFilter(List<String> predecessors, List<String> nodeNames) {
            super(predecessors);
            this.nodeNames = nodeNames;
        }

        @Override
        public boolean test(Node node) {
            return super.test(node) && (node.type() == Node.Type.GROUP || nodeNames.contains(node.name()));
        }
    }
    public static class CommonPredecessorAndGroupActionsInFilter extends CommonPredecessorFilter{
        private final List<String> groupNames;
        private final List<String> actionNames;
        public CommonPredecessorAndGroupActionsInFilter(List<String> predecessors, List<String> groupNames, List<String> actionNames) {
            super(predecessors);
            this.groupNames = groupNames;
            this.actionNames = actionNames;
        }

        @Override
        public boolean test(Node node) {
            return super.test(node) && (node.type() == Node.Type.GROUP ? groupNames.contains(node.name()) : actionNames.contains(node.name()));
        }
    }
    public static class CommonPredecessorAndGroupPatternFilter extends CommonPredecessorFilter{
        private final Pattern matchPattern;
        public CommonPredecessorAndGroupPatternFilter(List<String> predecessors, String pattern) {
            super(predecessors);
            matchPattern = Pattern.compile(pattern);
        }

        @Override
        public boolean test(Node node) {
            return super.test(node) && matchPattern.matcher(node.name()).matches();
        }
    }
    public static class CommonPredecessorAndActionPatternFilter extends CommonPredecessorFilter{
        private final Pattern matchPattern;
        public CommonPredecessorAndActionPatternFilter(List<String> predecessors, String pattern) {
            super(predecessors);
            matchPattern = Pattern.compile(pattern);
        }

        @Override
        public boolean test(Node node) {
            return super.test(node) && (node.type() == Node.Type.GROUP || matchPattern.matcher(node.name()).matches());
        }
    }
    public static class WhitelistFilter implements Predicate<Node>{
        protected final List<String> whitelist;

        public WhitelistFilter(List<String> whitelist) {
            this.whitelist = whitelist;
        }

        @Override
        public boolean test(Node node) {
            return whitelist.contains(node.name());
        }
    }
    public static class DirectoryWhitelistFilter extends WhitelistFilter{
        public DirectoryWhitelistFilter(List<String> whitelist) {
            super(whitelist);
        }

        @Override
        public boolean test(Node node) {
            return node.type() == Node.Type.GROUP && whitelist.contains(node.name());
        }
    }
    public static class FileWhitelistFilter extends WhitelistFilter{
        public FileWhitelistFilter(List<String> whitelist) {
            super(whitelist);
        }

        @Override
        public boolean test(Node node) {
            return node.type() == Node.Type.ACTION && whitelist.contains(node.name());
        }
    }
    public static class DirectoryAndFileWhitelistFilter implements Predicate<Node>{
        private final FileWhitelistFilter fileWhitelistFilter;
        private final DirectoryWhitelistFilter directoryWhitelistFilter;
        public DirectoryAndFileWhitelistFilter(List<String> dirList, List<String> fileList) {
            directoryWhitelistFilter = new DirectoryWhitelistFilter(dirList);
            fileWhitelistFilter = new FileWhitelistFilter(fileList);
        }

        @Override
        public boolean test(Node node) {
            return directoryWhitelistFilter.and(fileWhitelistFilter).test(node);
        }
    }
    public static class DirectoryOrFileWhitelistFilter implements Predicate<Node>{
        private final FileWhitelistFilter fileWhitelistFilter;
        private final DirectoryWhitelistFilter directoryWhitelistFilter;
        public DirectoryOrFileWhitelistFilter(List<String> dirList, List<String> fileList) {
            directoryWhitelistFilter = new DirectoryWhitelistFilter(dirList);
            fileWhitelistFilter = new FileWhitelistFilter(fileList);
        }

        @Override
        public boolean test(Node node) {
            return directoryWhitelistFilter.or(fileWhitelistFilter).test(node);
        }
    }
}
