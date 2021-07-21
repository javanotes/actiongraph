package org.reactiveminds.actiongraph.store;

import java.util.Optional;
import java.util.UUID;

public abstract class AbstractEventJournal implements EventJournal{
    protected abstract void recordEvent(ActionEntries entry);
    protected abstract ActionEntries fetchEvent(String correlationId);
    @Override
    public String createEntry(String root, String pathExpr, String payload) {
        ActionEntries entry = new ActionEntries();
        entry.setRoot(root);
        entry.setPayload(payload);
        entry.setPathMatcher(pathExpr == null || pathExpr.trim().isEmpty() ? "all" : pathExpr);
        entry.setStatus(EventJournal.STATUS_CREATED);
        entry.setCorrelationId(UUID.randomUUID().toString());
        entry.setCreated(System.currentTimeMillis());
        entry.setUpdated(entry.getCreated());
        recordEvent(entry);
        return entry.getCorrelationId();
    }
    private synchronized boolean updateEntry(String correlationId, String actionPath, String status, String addlInfo) {
        ActionEntries entry = fetchEvent(correlationId);
        if(entry != null){
            Optional<ActionEntry> actionEntry = entry.getByPath(actionPath);
            ActionEntry item;
            if(actionEntry.isPresent()){
                item = actionEntry.get();
            }
            else{
                item = new ActionEntry();
            }
            item.setStatus(status);
            item.setUpdated(System.currentTimeMillis());
            item.setRoot(entry.getRoot());
            item.setPayload(entry.getPayload());
            item.setPathMatcher(entry.getPathMatcher());
            item.setCorrelationId(entry.getCorrelationId());
            item.setCreated(entry.getCreated());
            item.setActionPath(actionPath);
            if(addlInfo != null)
                item.setAddlInfo(addlInfo);
            entry.addEntry(item);

            recordEvent(entry);
            return true;
        }
        return false;
    }

    @Override
    public ActionEntries getEntry(String corrId) {
        return fetchEvent(corrId);
    }

    @Override
    public boolean markSuccess(String correlationId, String actionPath) {
        return updateEntry(correlationId, actionPath, EventJournal.STATUS_SUCCESS, null);
    }

    @Override
    public boolean markFailed(String correlationId, String actionPath, String cause) {
        return updateEntry(correlationId, actionPath, EventJournal.STATUS_FAIL, cause);
    }
}
