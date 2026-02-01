package org.sokybot.commons.metrics;

/**
 * Base interface for all metric types.
 */
public interface IMetric {

    /**
     * Get the unique name of this metric.
     */
    String getName();

    /**
     * Get the description of what this metric measures.
     */
    String getDescription();

    /**
     * Get the metric type (counter, gauge, timer).
     */
    MetricType getType();

    /**
     * Get the current value as a number.
     */
    Number getValue();

    /**
     * Reset the metric to its initial state.
     */
    void reset();

    /**
     * Metric types.
     */
    enum MetricType {
        /** Monotonically increasing value */
        COUNTER,
        /** Value that can go up or down */
        GAUGE,
        /** Measures duration of operations */
        TIMER,
        /** Tracks distribution of values */
        HISTOGRAM
    }
}
