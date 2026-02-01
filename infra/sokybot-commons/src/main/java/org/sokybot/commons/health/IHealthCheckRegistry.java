package org.sokybot.commons.health;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for discovering and executing health checks.
 * 
 * Aggregates results from all registered health checks and provides
 * an overall system health status.
 */
public interface IHealthCheckRegistry {

    /**
     * Get all registered health check names.
     * 
     * @return list of health check names
     */
    List<String> getHealthCheckNames();

    /**
     * Get a specific health check by name.
     * 
     * @param name the health check name
     * @return the health check, if found
     */
    Optional<IHealthCheck> getHealthCheck(String name);

    /**
     * Run all health checks and return results.
     * 
     * @return map of component name to health check result
     */
    Map<String, HealthCheckResult> checkAll();

    /**
     * Run a specific health check by name.
     * 
     * @param name the health check name
     * @return the result, or empty if not found
     */
    Optional<HealthCheckResult> check(String name);

    /**
     * Get the overall system health status.
     * Returns the worst status from all checks.
     * 
     * @return aggregated health status
     */
    HealthStatus getOverallStatus();

    /**
     * Get cached results from the last check.
     * Returns empty map if no checks have been run.
     * 
     * @return cached results
     */
    Map<String, HealthCheckResult> getCachedResults();
}
