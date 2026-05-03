package org.sokybot.swarm.api;

import java.util.Objects;

/**
 * Epic #20 Sentinel Protocol: broadcast when a swarm bot is attacked by a hostile player.
 * {@link #getRequesterMachineId()} is the victim machine id; {@link #getAttackerCharacterName()} is normalized (trim + lowercase).
 */
public final class SwarmHostilePlayerEvent extends SwarmEvent {

    private final String attackerCharacterName;
    private final long incidentEpochMs;

    public SwarmHostilePlayerEvent(
            String victimMachineId,
            long timestampEpochMs,
            String requestId,
            String attackerCharacterName,
            long incidentEpochMs) {
        super(victimMachineId, timestampEpochMs, requestId);
        Objects.requireNonNull(attackerCharacterName, "attackerCharacterName");
        String normalized = attackerCharacterName.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("attackerCharacterName must be non-blank");
        }
        this.attackerCharacterName = normalized;
        this.incidentEpochMs = incidentEpochMs;
    }

    public String getAttackerCharacterName() {
        return attackerCharacterName;
    }

    public long getIncidentEpochMs() {
        return incidentEpochMs;
    }
}
