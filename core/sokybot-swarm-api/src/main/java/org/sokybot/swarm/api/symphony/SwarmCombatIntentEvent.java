package org.sokybot.swarm.api.symphony;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #21: broadcast when a bot starts the trigger skill of a symphony combo.
 */
public final class SwarmCombatIntentEvent extends SwarmEvent {

    private final String comboId;
    private final int triggerSkillRefId;
    private final int targetRefId;
    private final WorldPoint targetApproxPosition;
    private final long intentEpochMs;

    public SwarmCombatIntentEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            String comboId,
            int triggerSkillRefId,
            int targetRefId,
            WorldPoint targetApproxPosition,
            long intentEpochMs) {
        super(requesterMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(comboId, "comboId");
        if (comboId.trim().isEmpty()) {
            throw new IllegalArgumentException("comboId must be non-blank");
        }
        this.comboId = comboId.trim();
        this.triggerSkillRefId = triggerSkillRefId;
        this.targetRefId = targetRefId;
        this.targetApproxPosition = Objects.requireNonNull(targetApproxPosition, "targetApproxPosition");
        this.intentEpochMs = intentEpochMs;
    }

    public String getComboId() {
        return comboId;
    }

    public int getTriggerSkillRefId() {
        return triggerSkillRefId;
    }

    public int getTargetRefId() {
        return targetRefId;
    }

    public WorldPoint getTargetApproxPosition() {
        return targetApproxPosition;
    }

    public long getIntentEpochMs() {
        return intentEpochMs;
    }
}
