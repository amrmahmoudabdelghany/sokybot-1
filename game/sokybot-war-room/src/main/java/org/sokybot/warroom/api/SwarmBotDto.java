package org.sokybot.warroom.api;

import java.util.Objects;

import org.sokybot.party.api.shapeshifter.SwarmTacticalRole;

/**
 * Transport shape for War Room roster input (no Timefold / planning annotations).
 */
public final class SwarmBotDto {

    private final String machineId;
    private final int level;
    private final SwarmTacticalRole role;
    private final int dpsScore;

    public SwarmBotDto(String machineId, int level, SwarmTacticalRole role, int dpsScore) {
        this.machineId = Objects.requireNonNull(machineId, "machineId").trim();
        if (this.machineId.isEmpty()) {
            throw new IllegalArgumentException("machineId must not be blank");
        }
        this.level = level;
        this.role = Objects.requireNonNull(role, "role");
        this.dpsScore = dpsScore;
    }

    public String getMachineId() {
        return machineId;
    }

    public int getLevel() {
        return level;
    }

    public SwarmTacticalRole getRole() {
        return role;
    }

    public int getDpsScore() {
        return dpsScore;
    }
}
