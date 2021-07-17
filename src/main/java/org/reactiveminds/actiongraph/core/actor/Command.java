package org.reactiveminds.actiongraph.core.actor;

import org.reactiveminds.actiongraph.react.ActionMatcher;
import org.reactiveminds.actiongraph.react.Matchers;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public abstract class Command implements Serializable {
    public final String payload;
    public final ActionMatcher predicate;
    public final String correlationId;
    public abstract int type();
    public static final int STOP = 3;
    public static final int GROUP = 1;
    public static final int ACTION = 2;
    public static final int REPLAY = 0;
    /**
     *
     * @param eventType
     * @param payload
     * @param predicate
     * @return
     */
    public static Command newCommand(String correlationId, int eventType, String payload, ActionMatcher predicate){
        switch (eventType){
            case 1:
                return new GroupCommand(payload, predicate, correlationId);
            case 2:
                return new ActionCommand(payload, predicate, correlationId);
            case 3:
                return new StopCommand();
            default:
                throw new IllegalStateException("Unexpected event type id: " + eventType);
        }
    }

    protected Command(String payload, ActionMatcher predicate, String correlationId) {
        this.payload = payload;
        this.predicate = predicate;
        this.correlationId = correlationId;
    }
    public static class ReplayCommand extends Command {
        public final long originTime;
        public final Duration delay;
        public final int originType;
        public final int retryCount;

        @Override
        public String toString() {
            return "{" +
                    "originTime=" + new SimpleDateFormat("hh:mm:ss sss").format(new Date(originTime)) +
                    ", delay=" + delay +
                    ", originType=" + originType +
                    ", retryCount=" + retryCount +
                    '}';
        }

        ReplayCommand(ReplayCommand origin){
            super(origin.payload, origin.predicate, origin.correlationId);
            this.originType = origin.originType;
            this.originTime = System.currentTimeMillis();
            this.delay = Duration.ofMillis(Double.valueOf(origin.delay.toMillis() * NodeActor.RETRY_BACKOFF).longValue());
            this.retryCount = origin.retryCount+1;
        }
        /**
         *
         * @param origin
         * @param originTime
         * @param delay
         * @param retryCount
         */
        public ReplayCommand(Command origin, long originTime, Duration delay, int retryCount) {
            super(origin.payload, origin.predicate, origin.correlationId);
            this.originType = origin.type();
            this.originTime = originTime;
            this.delay = delay;
            this.retryCount = retryCount;
        }

        @Override
        public int type() {
            return REPLAY;
        }
    }
    static class GroupCommand extends Command {
        @Override
        public int type() {
            return GROUP;
        }

        GroupCommand(String payload, ActionMatcher predicate, String correlationId) {
            super(payload, predicate, correlationId);
        }
    }
    static class ActionCommand extends Command {
        @Override
        public int type() {
            return ACTION;
        }

        ActionCommand(String payload, ActionMatcher predicate, String correlationId) {
            super(payload, predicate, correlationId);
        }
    }
    public static class StopCommand extends Command {
        public StopCommand() {
            super("!#BANG", Matchers.ALL, "");
        }

        @Override
        public int type() {
            return STOP;
        }
    }
}
