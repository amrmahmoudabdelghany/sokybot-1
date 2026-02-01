package org.sokybot.commons.health.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.health.HealthCheckResult;
import org.sokybot.commons.health.HealthStatus;
import org.sokybot.commons.health.IHealthCheck;
import org.sokybot.commons.health.IHealthCheckRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi DS implementation of IHealthCheckRegistry.
 * 
 * Discovers health checks via whiteboard pattern and aggregates results.
 */
@Component(service = IHealthCheckRegistry.class, immediate = true)
public class HealthCheckRegistryImpl implements IHealthCheckRegistry {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckRegistryImpl.class);

    /**
     * Registered health checks (discovered via whiteboard).
     */
    private final List<IHealthCheck> healthChecks = new CopyOnWriteArrayList<>();

    /**
     * Cached results from last check.
     */
    private final Map<String, HealthCheckResult> cachedResults = new ConcurrentHashMap<>();

    @Activate
    protected void activate() {
        log.info("Health Check Registry activated with {} checks", healthChecks.size());
    }

    @Deactivate
    protected void deactivate() {
        log.info("Health Check Registry deactivating");
        cachedResults.clear();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindHealthCheck(IHealthCheck healthCheck) {
        healthChecks.add(healthCheck);
        log.info("Health check registered: {}", healthCheck.getName());
    }

    protected void unbindHealthCheck(IHealthCheck healthCheck) {
        healthChecks.remove(healthCheck);
        cachedResults.remove(healthCheck.getName());
        log.info("Health check unregistered: {}", healthCheck.getName());
    }

    @Override
    public List<String> getHealthCheckNames() {
        List<String> names = new ArrayList<>();
        for (IHealthCheck check : healthChecks) {
            names.add(check.getName());
        }
        return names;
    }

    @Override
    public Optional<IHealthCheck> getHealthCheck(String name) {
        for (IHealthCheck check : healthChecks) {
            if (check.getName().equals(name)) {
                return Optional.of(check);
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, HealthCheckResult> checkAll() {
        Map<String, HealthCheckResult> results = new LinkedHashMap<>();

        for (IHealthCheck check : healthChecks) {
            try {
                HealthCheckResult result = check.check();
                results.put(check.getName(), result);
                cachedResults.put(check.getName(), result);

                if (result.getStatus() != HealthStatus.HEALTHY) {
                    log.warn("Health check {} returned {}: {}",
                            check.getName(), result.getStatus(), result.getMessage());
                }
            } catch (Exception e) {
                log.error("Health check {} threw exception", check.getName(), e);
                HealthCheckResult errorResult = HealthCheckResult.unhealthy(
                        check.getName(), check.getCategory(), e);
                results.put(check.getName(), errorResult);
                cachedResults.put(check.getName(), errorResult);
            }
        }

        return results;
    }

    @Override
    public Optional<HealthCheckResult> check(String name) {
        return getHealthCheck(name).map(check -> {
            try {
                HealthCheckResult result = check.check();
                cachedResults.put(name, result);
                return result;
            } catch (Exception e) {
                log.error("Health check {} threw exception", name, e);
                HealthCheckResult errorResult = HealthCheckResult.unhealthy(
                        name, check.getCategory(), e);
                cachedResults.put(name, errorResult);
                return errorResult;
            }
        });
    }

    @Override
    public HealthStatus getOverallStatus() {
        if (cachedResults.isEmpty()) {
            // No checks run yet, run them now
            checkAll();
        }

        HealthStatus overall = HealthStatus.HEALTHY;
        for (HealthCheckResult result : cachedResults.values()) {
            overall = overall.combine(result.getStatus());
        }
        return overall;
    }

    @Override
    public Map<String, HealthCheckResult> getCachedResults() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(cachedResults));
    }
}
