package org.reactiveminds.actiongraph.core.actor;

import org.reactiveminds.actiongraph.react.ActionMatcher;
import org.reactiveminds.actiongraph.react.Matchers;

import java.io.Serializable;

public abstract class Event implements Serializable {
    public final String payload;
    public final ActionMatcher predicate;
    public abstract int id();
    public static final int STOP = 3;
    public static final int GROUP = 1;
    public static final int ACTION = 2;
    public static Event newEvent(int id, String payload, ActionMatcher predicate){
        switch (id){
            case 1:
                return new BranchEvent(payload, predicate);
            case 2:
                return new LeafEvent(payload, predicate);
            case 3:
                return new StopEvent();
            default:
                throw new IllegalStateException("Unexpected event type id: " + id);
        }
    }
    protected Event(String payload, ActionMatcher predicate) {
        this.payload = payload;
        this.predicate = predicate;
    }
    static class BranchEvent extends Event{
        @Override
        public int id() {
            return 1;
        }

        BranchEvent(String payload, ActionMatcher predicate) {
            super(payload, predicate);
        }
    }
    static class LeafEvent extends Event{
        @Override
        public int id() {
            return 2;
        }

        LeafEvent(String payload, ActionMatcher predicate) {
            super(payload, predicate);
        }
    }
    static class StopEvent extends Event{
        StopEvent() {
            super("!#BANG", Matchers.ALL);
        }

        @Override
        public int id() {
            return 3;
        }
    }
}
