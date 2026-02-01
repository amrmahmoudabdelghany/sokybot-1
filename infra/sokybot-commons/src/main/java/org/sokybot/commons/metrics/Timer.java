package org.sokybot.commons.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * A timer metric for measuring durations.
 * Tracks count, total time, min, max, and average.
 */
public class Timer implements IMetric {

    private final String name;
    private final String description;
    private final LongAdder count;
    private final LongAdder totalNanos;
    private volatile long minNanos = Long.MAX_VALUE;
    private volatile long maxNanos = 0;

    public Timer(String name, String description) {
        this.name = name;
        this.description = description;
        this.count = new LongAdder();
        this.totalNanos = new LongAdder();
    }

    /**
     * Record a duration.
     */
    public void record(long duration, TimeUnit unit) {
        long nanos = unit.toNanos(duration);
        count.increment();
        totalNanos.add(nanos);
        updateMinMax(nanos);
    }

    /**
     * Record a duration in nanoseconds.
     */
    public void recordNanos(long nanos) {
        count.increment();
        totalNanos.add(nanos);
        updateMinMax(nanos);
    }

    /**
     * Start timing and return a context that can be stopped.
     */
    public TimerContext start() {
        return new TimerContext(this);
    }

    /**
     * Execute a runnable and record its duration.
     */
    public void time(Runnable runnable) {
        long start = System.nanoTime();
        try {
            runnable.run();
        } finally {
            recordNanos(System.nanoTime() - start);
        }
    }

    private synchronized void updateMinMax(long nanos) {
        if (nanos < minNanos) {
            minNanos = nanos;
        }
        if (nanos > maxNanos) {
            maxNanos = nanos;
        }
    }

    /**
     * Get the number of recorded events.
     */
    public long getCount() {
        return count.sum();
    }

    /**
     * Get the total recorded time in milliseconds.
     */
    public double getTotalMillis() {
        return totalNanos.sum() / 1_000_000.0;
    }

    /**
     * Get the average duration in milliseconds.
     */
    public double getAverageMillis() {
        long c = count.sum();
        if (c == 0)
            return 0;
        return (totalNanos.sum() / 1_000_000.0) / c;
    }

    /**
     * Get the minimum recorded duration in milliseconds.
     */
    public double getMinMillis() {
        if (minNanos == Long.MAX_VALUE)
            return 0;
        return minNanos / 1_000_000.0;
    }

    /**
     * Get the maximum recorded duration in milliseconds.
     */
    public double getMaxMillis() {
        return maxNanos / 1_000_000.0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public MetricType getType() {
        return MetricType.TIMER;
    }

    @Override
    public Number getValue() {
        return getAverageMillis();
    }

    @Override
    public synchronized void reset() {
        count.reset();
        totalNanos.reset();
        minNanos = Long.MAX_VALUE;
        maxNanos = 0;
    }

    @Override
    public String toString() {
        return String.format("Timer{name='%s', count=%d, avg=%.2fms}",
                name, getCount(), getAverageMillis());
    }

    /**
     * Context for timing operations.
     */
    public static class TimerContext implements AutoCloseable {
        private final Timer timer;
        private final long startNanos;

        TimerContext(Timer timer) {
            this.timer = timer;
            this.startNanos = System.nanoTime();
        }

        /**
         * Stop the timer and record the duration.
         */
        public long stop() {
            long elapsed = System.nanoTime() - startNanos;
            timer.recordNanos(elapsed);
            return elapsed;
        }

        @Override
        public void close() {
            stop();
        }
    }
}
