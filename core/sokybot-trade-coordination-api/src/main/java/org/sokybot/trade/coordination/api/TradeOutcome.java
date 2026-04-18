package org.sokybot.trade.coordination.api;

/**
 * Terminal state for a coordinated trade session (logistics layer, not in-game packet outcome).
 */
public enum TradeOutcome {
    SUCCESS,
    FAILED,
    CANCELLED
}
