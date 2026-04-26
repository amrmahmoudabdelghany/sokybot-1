package org.sokybot.behaviors.trade.internal;

/**
 * Keys for {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()} trade flow.
 */
final class TradeKeys {

    static final String MULE_MACHINE_ID = "trade.muleMachineId";
    static final String COORDINATION_SESSION_ID = "trade.coordinationSessionId";
    static final String STALL_ENTITY_ID = "stall.entityId";
    /** Next inventory slot index to consider for swarm farmer fill (monotonic cursor). */
    static final String SWARM_FILL_SLOT_CURSOR = "trade.swarmFillSlotCursor";
    /** Next offer window slot index (0–11) for swarm farmer add-item. */
    static final String SWARM_OFFER_SLOT_CURSOR = "trade.swarmOfferSlotCursor";
    static final String STALL_EXCHANGE_CONFIRM_SENT = "stall.exchangeConfirmSent";
    static final String STALL_TRADE_WINDOW_OPENED = "stall.tradeWindowOpened";

    private TradeKeys() {
    }
}
