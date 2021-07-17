package org.reactiveminds.actiongraph.store;

public interface EventJournal {
    String STATUS_PENDING = "pending";
    String STATUS_SUCCESS = "success";
    String STATUS_FAIL = "fail";
    String createEntry(String root, String pathExpr, String payload);
    ActionEntry getEntry(String corrId);
    boolean markSuccess(String correlationId);
    boolean markFailed(String correlationId, String cause);
}
