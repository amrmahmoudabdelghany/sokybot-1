package org.sokybot.combat.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Next combat action to dispatch (auto-attack or skill use).
 */
public final class SkillAction {

    private final SkillActionKind kind;
    private final int skillRefId;
    private final int targetEntityId;
    private final Long earliestExecuteAtEpochMs;

    public SkillAction(SkillActionKind kind, int skillRefId, int targetEntityId, Long earliestExecuteAtEpochMs) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.skillRefId = skillRefId;
        this.targetEntityId = targetEntityId;
        this.earliestExecuteAtEpochMs = earliestExecuteAtEpochMs;
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
}
