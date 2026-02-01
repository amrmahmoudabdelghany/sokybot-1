package org.sokybot.commons.metrics.internal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.commons.metrics.Counter;
import org.sokybot.commons.metrics.Gauge;
import org.sokybot.commons.metrics.IMetric;
import org.sokybot.commons.metrics.IMetricsRegistry;
import org.sokybot.commons.metrics.Timer;

/**
 * Thread-safe implementation of the metrics registry.
 */
@Component(service = IMetricsRegistry.class)
public class MetricsRegistryImpl implements IMetricsRegistry {

    private static final Logger log = LoggerFactory.getLogger(MetricsRegistryImpl.class);

    private final Map<String, IMetric> metrics = new ConcurrentHashMap<>();

    @Override
    public Counter counter(String name, String description) {
        return (Counter) metrics.computeIfAbsent(name, n -> {
            log.debug("Creating counter: {}", name);
            return new Counter(n, description);
        });
    }

    @Override
    public Gauge gauge(String name, String description) {
        return (Gauge) metrics.computeIfAbsent(name, n -> {
            log.debug("Creating gauge: {}", name);
            return new Gauge(n, description);
        });
    }

    @Override
    public Gauge gauge(String name, String description, LongSupplier supplier) {
        return (Gauge) metrics.computeIfAbsent(name, n -> {
            log.debug("Creating bound gauge: {}", name);
            return new Gauge(n, description, supplier);
        });
    }

    @Override
    public Timer timer(String name, String description) {
        return (Timer) metrics.computeIfAbsent(name, n -> {
            log.debug("Creating timer: {}", name);
            return new Timer(n, description);
        });
    }

    @Override
    public Optional<IMetric> get(String name) {
        return Optional.ofNullable(metrics.get(name));
    }

    @Override
    public Map<String, IMetric> getAll() {
        return Map.copyOf(metrics);
    }

    @Override
    public Map<String, IMetric> getByPrefix(String prefix) {
        return metrics.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public boolean remove(String name) {
        IMetric removed = metrics.remove(name);
        if (removed != null) {
            log.debug("Removed metric: {}", name);
            return true;
        }
        return false;
    }

    @Override
    public void resetAll() {
        log.info("Resetting all {} metrics", metrics.size());
        metrics.values().forEach(IMetric::reset);
    }

    @Override
    public int size() {
        return metrics.size();
    }
}
