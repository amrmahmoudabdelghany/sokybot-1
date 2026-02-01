package org.sokybot.commons.error;

/**
 * Categories for classifying application errors.
 * Used for filtering and grouping errors in the UI.
 */
public enum ErrorCategory {

    /**
     * Network-related errors (connection, packet, proxy).
     */
    NETWORK,

    /**
     * Engine and workflow errors (actuator, cycle execution).
     */
    ENGINE,

    /**
     * Settings and configuration errors (validation, persistence).
     */
    SETTINGS,

    /**
     * System-level errors (OSGi, classloading, resources).
     */
    SYSTEM
}
