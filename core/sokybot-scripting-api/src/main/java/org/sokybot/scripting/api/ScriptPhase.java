package org.sokybot.scripting.api;

/**
 * High-level execution state for a machine running a travel script.
 */
public enum ScriptPhase {

    IDLE,
    WALKING,
    WAITING_TELEPORT_ACK,
    WAITING_LOAD_SCREEN,
    WAITING_PORTAL_ARRIVAL,
    SLEEPING,
    DONE,
    ERROR
}
