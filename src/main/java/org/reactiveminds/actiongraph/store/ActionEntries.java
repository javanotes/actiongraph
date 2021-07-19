package org.reactiveminds.actiongraph.store;

import java.io.Serializable;
import java.util.*;

public class ActionEntries extends ActionEntry implements Serializable {
    public List<ActionEntry> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public Optional<ActionEntry> getByPath(String path){
        return Optional.ofNullable(entries.get(path));
    }
    public void addEntry(ActionEntry entry) {
        entries.put(entry.getActionPath(), entry);
    }

    private Map<String, ActionEntry> entries = new LinkedHashMap<>();
}
