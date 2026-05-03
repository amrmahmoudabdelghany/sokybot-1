package org.sokybot.router.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: one swarm bot that may be designated as the mule.
 */
@PlanningEntity
public class SwarmBotEntity {

    @PlanningId
    private String machineId;

    private int maxCapacity;

    @PlanningVariable(valueRangeProviderRefs = "booleanRange")
    private Boolean isMule;

    public SwarmBotEntity() {
    }

    public SwarmBotEntity(String machineId, int maxCapacity, Boolean isMule) {
        this.machineId = machineId;
        this.maxCapacity = maxCapacity;
        this.isMule = isMule;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Boolean getIsMule() {
        return isMule;
    }

    public void setIsMule(Boolean isMule) {
        this.isMule = isMule;
    }

    @Override
    public String toString() {
        return machineId;
    }
}
