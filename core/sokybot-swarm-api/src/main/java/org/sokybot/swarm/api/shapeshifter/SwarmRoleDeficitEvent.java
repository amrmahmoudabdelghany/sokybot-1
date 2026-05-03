package org.sokybot.swarm.api.shapeshifter;

import java.util.Objects;

import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;
import org.sokybot.swarm.api.SwarmEvent;

/**
 * Epic #22: broadcast when a required tactical role is missing from the swarm/party view.
 */
public final class SwarmRoleDeficitEvent extends SwarmEvent {

    private final SwarmTacticalRole missingRole;
    private final String swarmGroupId;
    private final String missingMemberCharName;

    public SwarmRoleDeficitEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            SwarmTacticalRole missingRole,
            String swarmGroupId,
            String missingMemberCharName) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.missingRole = Objects.requireNonNull(missingRole, "missingRole");
        Objects.requireNonNull(swarmGroupId, "swarmGroupId");
        if (swarmGroupId.trim().isEmpty()) {
            throw new IllegalArgumentException("swarmGroupId must be non-blank");
        }
        this.swarmGroupId = swarmGroupId.trim();
        this.missingMemberCharName = Objects.requireNonNull(missingMemberCharName, "missingMemberCharName");
    }

    public SwarmTacticalRole getMissingRole() {
        return missingRole;
    }

    public String getSwarmGroupId() {
        return swarmGroupId;
    }

    public String getMissingMemberCharName() {
        return missingMemberCharName;
    }
}
