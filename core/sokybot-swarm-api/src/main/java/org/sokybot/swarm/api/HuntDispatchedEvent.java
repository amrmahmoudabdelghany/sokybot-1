package org.sokybot.swarm.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

public final class HuntDispatchedEvent extends SwarmEvent {

    private final String huntId;
    private final String hunterMachineId;
    private final int targetRefId;
    private final WorldPoint chosenPosition;
    private final long etaMs;

    public HuntDispatchedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String huntId,
            String hunterMachineId,
            int targetRefId,
            WorldPoint chosenPosition,
            long etaMs) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.huntId = Objects.requireNonNull(huntId, "huntId").trim();
        this.hunterMachineId = Objects.requireNonNull(hunterMachineId, "hunterMachineId").trim();
        this.targetRefId = targetRefId;
        this.chosenPosition = Objects.requireNonNull(chosenPosition, "chosenPosition");
        this.etaMs = etaMs;
    }

    public String getHuntId() {
        return huntId;
    }

    public String getHunterMachineId() {
        return hunterMachineId;
    }

    public int getTargetRefId() {
        return targetRefId;
    }

    public WorldPoint getChosenPosition() {
        return chosenPosition;
    }

    public long getEtaMs() {
        return etaMs;
    }
}
