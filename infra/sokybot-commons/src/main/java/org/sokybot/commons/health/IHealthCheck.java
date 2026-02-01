package org.sokybot.commons.health;

/**
 * Interface for components that provide health checks.
 * 
 * Implementations should be registered as OSGi services to be
 * automatically discovered by the health check registry.
 * 
 * Example:
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Component(service = IHealthCheck.class)
 *     public class DatabaseHealthCheck implements IHealthCheck {
 *         &#64;Override
 *         public String getName() {
 *             return "database";
 *         }
 * 
 *         &#64;Override
 *         public HealthCategory getCategory() {
 *             return HealthCategory.DATA;
 *         }
 * 
 *         @Override
 *         public HealthCheckResult check() {
 *             try {
 *                 // Check database connectivity
 *                 return HealthCheckResult.healthy(getName(), getCategory());
 *             } catch (Exception e) {
 *                 return HealthCheckResult.unhealthy(getName(), getCategory(), e);
 *             }
 *         }
 *     }
 * }
 * </pre>
 */
public interface IHealthCheck {

    /**
     * Get the unique name of this health check.
     * Used for identification and filtering.
     * 
     * @return component name (e.g., "database", "proxy", "engine")
     */
    String getName();

    /**
     * Get the category of this health check.
     * Used for grouping checks in the UI.
     * 
     * @return health check category
     */
    HealthCategory getCategory();

    /**
     * Perform the health check.
     * This method should be fast and non-blocking when possible.
     * 
     * @return health check result
     */
    HealthCheckResult check();

    /**
     * Get a description of what this health check verifies.
     * 
     * @return human-readable description
     */
    default String getDescription() {
        return "Health check for " + getName();
    }
}
