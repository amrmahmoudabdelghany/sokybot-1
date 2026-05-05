package org.sokybot.behaviors.logistics.router;

/**
 * Client-driven trade handshake state (rate-limited outbound steps).
 */
public enum TradeProtocolState {

    INIT,
    AWAITING_ACCEPT,
    ADDING_ITEM,
    AWAITING_ITEM_ECHO,
    CONFIRMING,
    AWAITING_CONFIRM,
    APPROVING,
    AWAITING_APPROVE,
    DONE,
    ABORTED
}
