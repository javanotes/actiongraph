package org.reactiveminds.actiongraph.store;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class GroupData implements Serializable {
    public List<String> getJsonContents() {
        return jsonContents;
    }

    public void setJsonContents(List<String> jsonContents) {
        this.jsonContents = jsonContents;
    }

    private List<String> jsonContents = new LinkedList<>();
}
