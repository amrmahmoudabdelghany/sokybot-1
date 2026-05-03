package org.sokybot.swarm.api.symphony;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #21: broadcast when the server confirms the combo status/debuff on the target.
 */
public final class SwarmCombatEffectEvent extends SwarmEvent {

    private final String comboId;
    private final int targetRefId;
    private final int statusRefId;
    private final WorldPoint targetApproxPosition;
    private final long effectEpochMs;

    public SwarmCombatEffectEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String comboId,
            int targetRefId,
            int statusRefId,
            WorldPoint targetApproxPosition,
            long effectEpochMs) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(comboId, "comboId");
        if (comboId.trim().isEmpty()) {
            throw new IllegalArgumentException("comboId must be non-blank");
        }
        this.comboId = comboId.trim();
        this.targetRefId = targetRefId;
        this.statusRefId = statusRefId;
        this.targetApproxPosition = Objects.requireNonNull(targetApproxPosition, "targetApproxPosition");
        this.effectEpochMs = effectEpochMs;
    }

    public String getComboId() {
        return comboId;
    }

    public int getTargetRefId() {
        return targetRefId;
    }

    public int getStatusRefId() {
        return statusRefId;
    }

    public WorldPoint getTargetApproxPosition() {
        return targetApproxPosition;
    }

    public long getEffectEpochMs() {
        return effectEpochMs;
    }
}
