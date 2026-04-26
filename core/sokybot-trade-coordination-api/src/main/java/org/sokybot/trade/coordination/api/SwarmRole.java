package org.sokybot.trade.coordination.api;

/**
 * Role of the local machine in a coordinated swarm (P2P) trade session.
 */
public enum SwarmRole {
    NONE,
    FARMER,
    MULE,
    HUNTER,
    BOTH
}
