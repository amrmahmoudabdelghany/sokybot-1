package org.sokybot.swarm.api;

import java.util.Objects;

/**
 * Emitted when a mule wins the CAS claim for a logistics request.
 */
public final class LogisticsClaimedEvent extends SwarmEvent {

    private final String muleMachineId;
    /** In-game unique id of the mule character (for matching {@code TradeWindowOpened}). */
    private final long muleTrainerUniqueId;

    public LogisticsClaimedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String muleMachineId) {
        this(requesterMachineId, timestampEpochMs, requestId, muleMachineId, 0L);
    }

    public LogisticsClaimedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String muleMachineId,
            long muleTrainerUniqueId) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.muleMachineId = Objects.requireNonNull(muleMachineId, "muleMachineId").trim();
        this.muleTrainerUniqueId = muleTrainerUniqueId;
    }

    public String getMuleMachineId() {
        return muleMachineId;
    }

    public long getMuleTrainerUniqueId() {
        return muleTrainerUniqueId;
    }
}
