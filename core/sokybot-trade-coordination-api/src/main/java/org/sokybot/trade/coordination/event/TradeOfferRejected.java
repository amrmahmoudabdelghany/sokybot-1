package org.sokybot.trade.coordination.event;

import org.sokybot.engine.api.EngineEvent;

import java.util.Objects;

/**
 * Mule (or peer) rejected the coordination session.
 */
public final class TradeOfferRejected extends EngineEvent {

    private final String sessionId;
    private final String respondingMachineId;
    private final String reason;

    public TradeOfferRejected(String sessionId, String respondingMachineId, String reason) {
        super("TRADE_OFFER_REJECTED");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.respondingMachineId = Objects.requireNonNull(respondingMachineId, "respondingMachineId");
        this.reason = reason != null ? reason : "";
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRespondingMachineId() {
        return respondingMachineId;
    }

    public String getReason() {
        return reason;
    }
}
