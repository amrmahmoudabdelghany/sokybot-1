package org.sokybot.grid.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable grid assignment: machine id → assigned hold node.
 */
public final class GridPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, GridNodeDto> botAssignments;

    public GridPlan(Map<String, GridNodeDto> botAssignments) {
        Objects.requireNonNull(botAssignments, "botAssignments");
        Map<String, GridNodeDto> copy = new LinkedHashMap<>();
        for (Map.Entry<String, GridNodeDto> e : botAssignments.entrySet()) {
            String mid = e.getKey();
            GridNodeDto node = e.getValue();
            Objects.requireNonNull(mid, "machineId");
            Objects.requireNonNull(node, "gridNode");
            copy.put(mid, node);
        }
        this.botAssignments = Collections.unmodifiableMap(copy);
    }

    public Map<String, GridNodeDto> getBotAssignments() {
        return botAssignments;
    }
}
