package org.sokybot.grid.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

/**
 * Problem fact: one discrete grid node at map coordinates.
 */
public class GridNode {

    @PlanningId
    private String nodeId;

    private double x;

    private double y;

    public GridNode() {
    }

    public GridNode(String nodeId, double x, double y) {
        this.nodeId = nodeId;
        this.x = x;
        this.y = y;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return nodeId;
    }
}
