package org.sokybot.swarm.api;

/**
 * Phase of a coordinated lure cycle (Epic #14 — synchronized swarm luring).
 */
public enum SwarmLurePhase {
    FAN_OUT,
    PULLING,
    CONVERGING,
    ARRIVED
}
