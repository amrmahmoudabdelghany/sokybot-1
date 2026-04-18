package org.sokybot.behaviors.trade.internal;

/**
 * Keys for {@link org.sokybot.engine.api.workflow.IWorkflowContext#getPersistentData()} trade flow.
 */
final class TradeKeys {

    static final String MULE_MACHINE_ID = "trade.muleMachineId";
    static final String COORDINATION_SESSION_ID = "trade.coordinationSessionId";
    static final String STALL_ENTITY_ID = "stall.entityId";

    private TradeKeys() {
    }
}
