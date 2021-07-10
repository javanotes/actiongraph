package org.reactiveminds.actiongraph.core.actor;

import org.reactiveminds.actiongraph.core.Node;
import org.reactiveminds.actiongraph.react.Predicates;

import java.io.Serializable;
import java.util.function.Predicate;

public abstract class Event implements Serializable {
    public final String payload;
    public final Predicate<Node> predicate;
    public abstract int id();
    public static final int STOP = 3;
    public static final int GROUP = 1;
    public static final int ACTION = 2;
    public static Event newEvent(int id, String payload, Predicate<Node> predicate){
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
    protected Event(String payload, Predicate<Node> predicate) {
        this.payload = payload;
        this.predicate = predicate;
    }
    static class BranchEvent extends Event{
        @Override
        public int id() {
            return 1;
        }

        BranchEvent(String payload, Predicate<Node> predicate) {
            super(payload, predicate);
        }
    }
    static class LeafEvent extends Event{
        @Override
        public int id() {
            return 2;
        }

        LeafEvent(String payload, Predicate<Node> predicate) {
            super(payload, predicate);
        }
    }
    static class StopEvent extends Event{
        StopEvent() {
            super("!#BANG", Predicates.MATCH_ALL);
        }

        @Override
        public int id() {
            return 3;
        }
    }
}
