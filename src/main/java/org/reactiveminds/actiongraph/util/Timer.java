package org.reactiveminds.actiongraph.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Timer {
    private volatile long started;
    private volatile long stopped;
    private boolean isStopped = true;
    private volatile int reset = 0;
    private final long created;
    public Timer() {
        created = System.nanoTime();
    }
    public void stop(){
        if(isStopped)
            return;
        stopped = System.nanoTime();
        isStopped = true;
    }
    public void start(){
        if(!isStopped)
            return;
        started = System.nanoTime();
        stopped = started;
        isStopped = false;
        reset++;
    }
    public void halt(long fromTime, Duration until){
        try {
            Thread.sleep(fromTime + until.toMillis() - System.currentTimeMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (IllegalArgumentException e) {
            //ignore
        }
    }
    public long lastLap(TimeUnit unit){
        return unit.convert(stopped - started, TimeUnit.NANOSECONDS);
    }
    public long overall(TimeUnit unit){
        return unit.convert(System.nanoTime() - created, TimeUnit.NANOSECONDS);
    }
}
