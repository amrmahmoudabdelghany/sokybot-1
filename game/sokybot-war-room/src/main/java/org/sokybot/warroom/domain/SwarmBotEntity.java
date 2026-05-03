package org.sokybot.warroom.domain;

import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: one swarm bot assigned to at most one {@link WarRoomParty}.
 */
@PlanningEntity
public class SwarmBotEntity {

    @PlanningId
    private String machineId;

    private int level;

    private SwarmTacticalRole role;

    private int dpsScore;

    @PlanningVariable(valueRangeProviderRefs = "partyRange")
    private WarRoomParty assignedParty;

    public SwarmBotEntity() {
    }

    public SwarmBotEntity(
            String machineId,
            int level,
            SwarmTacticalRole role,
            int dpsScore,
            WarRoomParty assignedParty) {
        this.machineId = machineId;
        this.level = level;
        this.role = role;
        this.dpsScore = dpsScore;
        this.assignedParty = assignedParty;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public SwarmTacticalRole getRole() {
        return role;
    }

    public void setRole(SwarmTacticalRole role) {
        this.role = role;
    }

    public int getDpsScore() {
        return dpsScore;
    }

    public void setDpsScore(int dpsScore) {
        this.dpsScore = dpsScore;
    }

    public WarRoomParty getAssignedParty() {
        return assignedParty;
    }

    public void setAssignedParty(WarRoomParty assignedParty) {
        this.assignedParty = assignedParty;
    }

    @Override
    public String toString() {
        return machineId;
    }
}
