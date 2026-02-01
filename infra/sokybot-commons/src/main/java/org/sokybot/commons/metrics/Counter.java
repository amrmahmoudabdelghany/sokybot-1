package org.sokybot.commons.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * A counter metric that can only increase.
 * Thread-safe and optimized for high-frequency updates.
 */
public class Counter implements IMetric {

    private final String name;
    private final String description;
    private final LongAdder count;

    public Counter(String name, String description) {
        this.name = name;
        this.description = description;
        this.count = new LongAdder();
    }

    /**
     * Increment the counter by 1.
     */
    public void increment() {
        count.increment();
    }

    /**
     * Increment the counter by the given amount.
     */
    public void increment(long amount) {
        count.add(amount);
    }

    /**
     * Get the current count.
     */
    public long getCount() {
        return count.sum();
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
        return MetricType.COUNTER;
    }

    @Override
    public Number getValue() {
        return getCount();
    }

    @Override
    public void reset() {
        count.reset();
    }

    @Override
    public String toString() {
        return String.format("Counter{name='%s', count=%d}", name, getCount());
    }
}
