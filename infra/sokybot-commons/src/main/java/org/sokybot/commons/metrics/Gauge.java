package org.sokybot.commons.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * A gauge metric that can go up or down.
 * Can be set directly or bound to a supplier.
 */
public class Gauge implements IMetric {

    private final String name;
    private final String description;
    private final AtomicLong value;
    private volatile LongSupplier supplier;

    public Gauge(String name, String description) {
        this.name = name;
        this.description = description;
        this.value = new AtomicLong(0);
    }

    public Gauge(String name, String description, LongSupplier supplier) {
        this.name = name;
        this.description = description;
        this.value = new AtomicLong(0);
        this.supplier = supplier;
    }

    /**
     * Set the gauge to a specific value.
     */
    public void set(long newValue) {
        value.set(newValue);
    }

    /**
     * Increment the gauge by 1.
     */
    public void increment() {
        value.incrementAndGet();
    }

    /**
     * Decrement the gauge by 1.
     */
    public void decrement() {
        value.decrementAndGet();
    }

    /**
     * Add to the gauge value.
     */
    public void add(long amount) {
        value.addAndGet(amount);
    }

    /**
     * Bind this gauge to a supplier function.
     * When getValue is called, the supplier will be invoked.
     */
    public void bind(LongSupplier supplier) {
        this.supplier = supplier;
    }

    /**
     * Get the current gauge value.
     */
    public long get() {
        if (supplier != null) {
            return supplier.getAsLong();
        }
        return value.get();
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
        return MetricType.GAUGE;
    }

    @Override
    public Number getValue() {
        return get();
    }

    @Override
    public void reset() {
        value.set(0);
        supplier = null;
    }

    @Override
    public String toString() {
        return String.format("Gauge{name='%s', value=%d}", name, get());
    }
}
