package org.sokybot.trade.coordination.event;

import org.sokybot.engine.api.EngineEvent;
import org.sokybot.trade.coordination.api.TradeOutcome;

import java.util.Objects;

/**
 * Logistics session finished (after in-game trade may have succeeded or failed separately).
 */
public final class TradeCompleted extends EngineEvent {

    private final String sessionId;
    private final TradeOutcome outcome;
    private final String farmerMachineId;
    private final String muleMachineId;

    public TradeCompleted(String sessionId,
            TradeOutcome outcome,
            String farmerMachineId,
            String muleMachineId) {
        super("TRADE_COORDINATION_COMPLETED");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.farmerMachineId = Objects.requireNonNull(farmerMachineId, "farmerMachineId");
        this.muleMachineId = Objects.requireNonNull(muleMachineId, "muleMachineId");
    }

    public String getSessionId() {
        return sessionId;
    }

    public TradeOutcome getOutcome() {
        return outcome;
    }

    public String getFarmerMachineId() {
        return farmerMachineId;
    }

    public String getMuleMachineId() {
        return muleMachineId;
    }
}
