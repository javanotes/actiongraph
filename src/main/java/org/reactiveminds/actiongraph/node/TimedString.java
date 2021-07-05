package org.reactiveminds.actiongraph.node;

import java.util.Objects;

class TimedString implements Comparable<TimedString> {
    private final String text;
    private final long time;

    public String getText() {
        return text;
    }

    public long getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimedString that = (TimedString) o;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    TimedString(String text) {
        if (text == null)
            throw new IllegalArgumentException("null not allowed");
        this.text = text;
        time = System.nanoTime();
    }

    @Override
    public int compareTo(TimedString o) {
        return Long.compare(this.time, o.time);
    }
}
