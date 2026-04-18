package org.sokybot.trade.coordination.api;

import java.util.Optional;

/**
 * Process-wide registry and session broker for multi-bot trade coordination.
 * Implementations relay {@link org.sokybot.engine.api.EngineEvent}s via {@code IEngineEventMediator}.
 */
public interface ITradeCoordinator {

    /**
     * Registers a mule bot; returns an opaque ticket id (idempotent replace if same machine re-registers).
     */
    String registerMule(MuleIntent intent);

    void unregisterMule(String machineId);

    /**
     * Selects a suitable mule for the farmer (v1: first other bot with free slots, FIFO by registration).
     */
    Optional<MuleHandle> findBestMule(FarmerProfile profile);

    /**
     * Opens a coordination session and broadcasts {@code TradeOfferProposed} to all engines (listeners filter by
     * target id).
     *
     * @return session id
     */
    String openTradeSession(String farmerMachineId, String muleMachineId);

    void acceptTradeSession(String sessionId, String respondingMachineId);

    void rejectTradeSession(String sessionId, String respondingMachineId, String reason);

    void completeTradeSession(String sessionId, TradeOutcome outcome);

    /**
     * Announces that a mule has reached stall-ready state (listing phase).
     */
    void publishStallReady(String machineId, String stallId);
}
