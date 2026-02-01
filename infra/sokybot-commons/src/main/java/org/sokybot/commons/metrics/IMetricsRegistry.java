package org.sokybot.commons.metrics;

import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Central registry for metrics.
 * Provides factory methods for creating and retrieving metrics.
 */
public interface IMetricsRegistry {

    /**
     * Get or create a counter with the given name.
     * 
     * @param name        unique metric name (e.g., "packets.sent")
     * @param description what this counter measures
     * @return the counter
     */
    Counter counter(String name, String description);

    /**
     * Get or create a gauge with the given name.
     * 
     * @param name        unique metric name
     * @param description what this gauge measures
     * @return the gauge
     */
    Gauge gauge(String name, String description);

    /**
     * Get or create a gauge bound to a supplier.
     * 
     * @param name        unique metric name
     * @param description what this gauge measures
     * @param supplier    function that provides the current value
     * @return the gauge
     */
    Gauge gauge(String name, String description, LongSupplier supplier);

    /**
     * Get or create a timer with the given name.
     * 
     * @param name        unique metric name
     * @param description what this timer measures
     * @return the timer
     */
    Timer timer(String name, String description);

    /**
     * Get a metric by name.
     * 
     * @param name the metric name
     * @return the metric, or empty if not found
     */
    Optional<IMetric> get(String name);

    /**
     * Get all registered metrics.
     * 
     * @return map of name to metric
     */
    Map<String, IMetric> getAll();

    /**
     * Get all metrics matching a prefix.
     * 
     * @param prefix the prefix to match (e.g., "packets.")
     * @return map of name to metric
     */
    Map<String, IMetric> getByPrefix(String prefix);

    /**
     * Remove a metric from the registry.
     * 
     * @param name the metric name
     * @return true if removed
     */
    boolean remove(String name);

    /**
     * Reset all metrics to their initial values.
     */
    void resetAll();

    /**
     * Get the number of registered metrics.
     */
    int size();
}
