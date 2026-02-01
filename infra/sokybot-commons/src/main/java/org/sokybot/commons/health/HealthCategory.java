package org.sokybot.commons.health;

/**
 * Category for grouping health checks.
 */
public enum HealthCategory {

    /**
     * Core system components (engine, runtime).
     */
    CORE,

    /**
     * Network components (proxy, connections).
     */
    NETWORK,

    /**
     * Data components (database, persistence).
     */
    DATA,

    /**
     * External integrations (game servers).
     */
    EXTERNAL
}
