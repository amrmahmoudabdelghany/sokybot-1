package org.sokybot.trade.coordination.event;

import org.sokybot.engine.api.EngineEvent;

import java.util.Objects;

/**
 * Mule (or peer) accepted the coordination session.
 */
public final class TradeOfferAccepted extends EngineEvent {

    private final String sessionId;
    private final String respondingMachineId;

    public TradeOfferAccepted(String sessionId, String respondingMachineId) {
        super("TRADE_OFFER_ACCEPTED");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.respondingMachineId = Objects.requireNonNull(respondingMachineId, "respondingMachineId");
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRespondingMachineId() {
        return respondingMachineId;
    }
}
