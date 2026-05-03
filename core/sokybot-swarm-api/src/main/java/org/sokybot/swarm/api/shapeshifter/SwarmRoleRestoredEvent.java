package org.sokybot.swarm.api.shapeshifter;

import java.util.Objects;

import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;
import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #22: broadcast when a previously missing tactical role is covered again.
 */
public final class SwarmRoleRestoredEvent extends SwarmEvent {

    private final SwarmTacticalRole restoredRole;
    private final String swarmGroupId;
    private final String restoredMemberCharName;

    public SwarmRoleRestoredEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            SwarmTacticalRole restoredRole,
            String swarmGroupId,
            String restoredMemberCharName) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.restoredRole = Objects.requireNonNull(restoredRole, "restoredRole");
        Objects.requireNonNull(swarmGroupId, "swarmGroupId");
        if (swarmGroupId.trim().isEmpty()) {
            throw new IllegalArgumentException("swarmGroupId must be non-blank");
        }
        this.swarmGroupId = swarmGroupId.trim();
        this.restoredMemberCharName = Objects.requireNonNull(restoredMemberCharName, "restoredMemberCharName");
    }

    public SwarmTacticalRole getRestoredRole() {
        return restoredRole;
    }

    public String getSwarmGroupId() {
        return swarmGroupId;
    }

    public String getRestoredMemberCharName() {
        return restoredMemberCharName;
    }
}
