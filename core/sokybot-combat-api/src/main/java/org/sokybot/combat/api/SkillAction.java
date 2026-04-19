package org.sokybot.combat.api;

import java.util.Objects;
import java.util.Optional;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Next combat action to dispatch (auto-attack or skill use).
 */
public final class SkillAction {

    private final SkillActionKind kind;
    private final int skillRefId;
    private final int targetEntityId;
    private final Long earliestExecuteAtEpochMs;
    private final Optional<WorldPoint> aoeTarget;

    public SkillAction(SkillActionKind kind, int skillRefId, int targetEntityId, Long earliestExecuteAtEpochMs) {
        this(kind, skillRefId, targetEntityId, earliestExecuteAtEpochMs, Optional.empty());
    }

    public SkillAction(SkillActionKind kind, int skillRefId, int targetEntityId, Long earliestExecuteAtEpochMs,
            Optional<WorldPoint> aoeTarget) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.skillRefId = skillRefId;
        this.targetEntityId = targetEntityId;
        this.earliestExecuteAtEpochMs = earliestExecuteAtEpochMs;
        this.aoeTarget = aoeTarget != null ? aoeTarget : Optional.empty();
    }

    public SkillActionKind getKind() {
        return kind;
    }

    /** Zero when not applicable (e.g. pure auto-attack without a skill ref). */
    public int getSkillRefId() {
        return skillRefId;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public Optional<Long> getEarliestExecuteAtEpochMs() {
        return Optional.ofNullable(earliestExecuteAtEpochMs);
    }

    /** When present, skill should be cast at this world point (AoE / ground targeting). */
    public Optional<WorldPoint> getAoeTarget() {
        return aoeTarget;
    }
}
