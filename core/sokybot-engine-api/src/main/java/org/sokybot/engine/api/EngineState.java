package org.sokybot.engine.api;

/**
 * Engine state enumeration.
 * Represents the lifecycle state of the engine.
 */
public enum EngineState {
    /** Engine not started */
    STOPPED,
    /** Engine running, cycle inactive */
    IDLE,
    /** Engine running, cycle active */
    ACTIVE
}
