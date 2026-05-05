package org.sokybot.grid.api;

import java.io.Serializable;
import java.util.Objects;

/**
 * Hunting Grid solver input for one bot (no Timefold types).
 */
public final class GridBotDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String machineId;
    private final double attackRange;
    private final boolean buffer;

    public GridBotDto(String machineId, double attackRange, boolean buffer) {
        this.machineId = Objects.requireNonNull(machineId, "machineId").trim();
        if (this.machineId.isEmpty()) {
            throw new IllegalArgumentException("machineId must not be blank");
        }
        this.attackRange = attackRange;
        this.buffer = buffer;
    }

    public String getMachineId() {
        return machineId;
    }

    public double getAttackRange() {
        return attackRange;
    }

    public boolean isBuffer() {
        return buffer;
    }
}
