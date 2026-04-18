package org.sokybot.trade.coordination.event;

import org.sokybot.engine.api.EngineEvent;

import java.util.Objects;

/**
 * Point-to-point coordination: farmer proposes a trade session to a specific mule.
 * Listeners should handle only when {@link #getTargetMachineId()} matches their engine.
 */
public final class TradeOfferProposed extends EngineEvent {

    private final String sessionId;
    private final String sourceMachineId;
    private final String targetMachineId;

    public TradeOfferProposed(String sessionId, String sourceMachineId, String targetMachineId) {
        super("TRADE_OFFER_PROPOSED");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.sourceMachineId = Objects.requireNonNull(sourceMachineId, "sourceMachineId");
        this.targetMachineId = Objects.requireNonNull(targetMachineId, "targetMachineId");
    }

    public String getSessionId() {
        return sessionId;
    }

    /** Farmer / initiator full machine id (group.machine). */
    public String getSourceMachineId() {
        return sourceMachineId;
    }

    /** Mule full machine id (group.machine). */
    public String getTargetMachineId() {
        return targetMachineId;
    }
}
