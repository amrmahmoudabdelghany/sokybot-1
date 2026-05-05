package org.sokybot.grid.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Planning entity: one swarm bot assigned to at most one {@link GridNode}.
 */
@PlanningEntity
public class GridBotEntity {

    @PlanningId
    private String machineId;

    private double attackRange;

    private boolean buffer;

    @PlanningVariable(valueRangeProviderRefs = "nodeRange")
    private GridNode assignedNode;

    public GridBotEntity() {
    }

    public GridBotEntity(
            String machineId,
            double attackRange,
            boolean buffer,
            GridNode assignedNode) {
        this.machineId = machineId;
        this.attackRange = attackRange;
        this.buffer = buffer;
        this.assignedNode = assignedNode;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public void setAttackRange(double attackRange) {
        this.attackRange = attackRange;
    }

    public boolean isBuffer() {
        return buffer;
    }

    public void setBuffer(boolean buffer) {
        this.buffer = buffer;
    }

    public GridNode getAssignedNode() {
        return assignedNode;
    }

    public void setAssignedNode(GridNode assignedNode) {
        this.assignedNode = assignedNode;
    }

    @Override
    public String toString() {
        return machineId;
    }
}
