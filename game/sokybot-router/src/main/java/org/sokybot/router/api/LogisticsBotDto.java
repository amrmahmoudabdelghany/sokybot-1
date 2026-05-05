package org.sokybot.router.api;

import java.util.Objects;

/**
 * Router logistics input: one bot carrying capacity (no Timefold types).
 */
public final class LogisticsBotDto {

    private final String machineId;
    private final int maxCapacity;

    public LogisticsBotDto(String machineId, int maxCapacity) {
        this.machineId = Objects.requireNonNull(machineId, "machineId").trim();
        if (this.machineId.isEmpty()) {
            throw new IllegalArgumentException("machineId must not be blank");
        }
        this.maxCapacity = maxCapacity;
    }

    public String getMachineId() {
        return machineId;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }
}
