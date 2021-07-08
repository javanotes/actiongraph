package org.reactiveminds.actiongraph.store;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class GroupData implements Serializable {
    public static final String FIELD_ROOT = "root";
    public static final String FIELD_GRAPH = "graph";
    private String root;
    public List<String> getGraphs() {
        return graphs;
    }

    public void setGraphs(List<String> graphs) {
        this.graphs = graphs;
    }

    private List<String> graphs = new LinkedList<>();

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
