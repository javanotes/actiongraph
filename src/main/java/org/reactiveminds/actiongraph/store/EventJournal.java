package org.reactiveminds.actiongraph.store;

import java.util.List;

public interface EventJournal {
    String STATUS_CREATED = "created";
    String STATUS_SUCCESS = "success";
    String STATUS_FAIL = "fail";
    String createEntry(String root, String pathExpr, String payload);
    ActionEntries getEntry(String corrId);
    boolean markSuccess(String correlationId, String actionPath);
    boolean markFailed(String correlationId, String actionPath, String cause);
}
