package org.sokybot.trade.coordination.api;

/**
 * Outcome reported when closing a swarm session.
 */
public enum SwarmSessionResult {
    COMPLETED,
    ABORTED,
    CANCELLED,
    ERROR
}
