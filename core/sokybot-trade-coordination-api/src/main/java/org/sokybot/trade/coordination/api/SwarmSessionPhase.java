package org.sokybot.trade.coordination.api;

/**
 * High-level phase for a swarm trade session on one side of the exchange.
 */
public enum SwarmSessionPhase {
    REQUESTING,
    OPEN,
    FILLING,
    ACCEPTED,
    CONFIRMED,
    COMPLETE,
    ABORTED
}
