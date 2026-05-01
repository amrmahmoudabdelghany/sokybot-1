package org.sokybot.swarm.api;

/**
 * Broadcast when a bot engages a combat target so phalanx followers can mirror with minimal latency.
 */
public final class SwarmTargetEngagedEvent extends SwarmEvent {

    public enum Kind {
        AUTO_ATTACK,
        ATTACK_SKILL
    }

    private final Kind kind;
    private final int targetEntityId;
    private final int targetRefId;
    private final int skillRefId;
    private final float sourceX;
    private final float sourceY;
    private final float sourceZ;
    private final float targetX;
    private final float targetY;
    private final float targetZ;

    public SwarmTargetEngagedEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            Kind kind,
            int targetEntityId,
            int targetRefId,
            int skillRefId,
            float sourceX,
            float sourceY,
            float sourceZ,
            float targetX,
            float targetY,
            float targetZ) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.kind = kind != null ? kind : Kind.AUTO_ATTACK;
        this.targetEntityId = targetEntityId;
        this.targetRefId = targetRefId;
        this.skillRefId = skillRefId;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceZ = sourceZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public Kind getKind() {
        return kind;
    }

    public int getTargetEntityId() {
        return targetEntityId;
    }

    public int getTargetRefId() {
        return targetRefId;
    }

    public int getSkillRefId() {
        return skillRefId;
    }

    public float getSourceX() {
        return sourceX;
    }

    public float getSourceY() {
        return sourceY;
    }

    public float getSourceZ() {
        return sourceZ;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getTargetZ() {
        return targetZ;
    }

    public String getLeaderMachineFullName() {
        return getRequesterMachineId();
    }
}
