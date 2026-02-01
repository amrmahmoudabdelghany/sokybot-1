package org.sokybot.commons.health;

/**
 * Status indicator for health checks.
 */
public enum HealthStatus {

    /**
     * Component is fully operational.
     */
    HEALTHY,

    /**
     * Component has issues but is still functional.
     * May indicate degraded performance or partial failure.
     */
    DEGRADED,

    /**
     * Component has failed and is not operational.
     */
    UNHEALTHY;

    /**
     * Returns the more severe of two statuses.
     * Used for aggregating multiple health check results.
     * 
     * @param other the other status to compare
     * @return the more severe status
     */
    public HealthStatus combine(HealthStatus other) {
        if (this == UNHEALTHY || other == UNHEALTHY) {
            return UNHEALTHY;
        }
        if (this == DEGRADED || other == DEGRADED) {
            return DEGRADED;
        }
        return HEALTHY;
    }
}
