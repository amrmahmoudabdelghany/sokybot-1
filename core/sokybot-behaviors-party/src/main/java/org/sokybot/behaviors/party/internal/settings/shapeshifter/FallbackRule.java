package org.sokybot.behaviors.party.internal.settings.shapeshifter;

import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;

import lombok.Data;

/**
 * Epic #22: one fallback mapping when a tactical role is missing from the party.
 */
@Data
public class FallbackRule {

    private SwarmTacticalRole deficitRole = SwarmTacticalRole.UNKNOWN;

    private String backupMachineName = "";

    private int weaponItemRefId;

    private String flexCycleId;

    private String baselineCycleId;

    private String assignedCharacterName = "";
}
