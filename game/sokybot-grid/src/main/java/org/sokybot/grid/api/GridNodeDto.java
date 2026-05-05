package org.sokybot.grid.api;

import java.io.Serializable;
import java.util.Objects;

/**
 * Hunting Grid solver input for one discrete node (no Timefold types).
 */
public final class GridNodeDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nodeId;
    private final double x;
    private final double y;

    public GridNodeDto(String nodeId, double x, double y) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId").trim();
        if (this.nodeId.isEmpty()) {
            throw new IllegalArgumentException("nodeId must not be blank");
        }
        this.x = x;
        this.y = y;
    }

    public String getNodeId() {
        return nodeId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
