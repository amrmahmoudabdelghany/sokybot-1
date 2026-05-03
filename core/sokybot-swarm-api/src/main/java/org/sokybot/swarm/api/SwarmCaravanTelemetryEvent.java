package org.sokybot.swarm.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * Epic #18 Caravan Syndicate: Trader broadcasts pose and threat hints for Hunter escorts.
 * {@link #getRequesterMachineId()} is the Trader machine id; {@link #getRequestId()} correlates this ping.
 */
public final class SwarmCaravanTelemetryEvent extends SwarmEvent {

    private static final WorldPoint UNKNOWN_ATTACKER_POSITION = new WorldPoint(0.0f, 0.0f, 0.0f);

    private final String caravanId;
    private final WorldPoint traderPosition;
    private final float velX;
    private final float velY;
    private final float velZ;
    private final boolean underAttack;
    private final int attackerRefId;
    private final WorldPoint attackerApproxPosition;

    public SwarmCaravanTelemetryEvent(
            String traderMachineId,
            long timestampEpochMs,
            String requestId,
            String caravanId,
            WorldPoint traderPosition,
            float velX,
            float velY,
            float velZ,
            boolean underAttack,
            int attackerRefId,
            WorldPoint attackerApproxPosition) {
        super(traderMachineId, timestampEpochMs, requestId);
        this.caravanId = caravanId == null ? "" : caravanId.trim();
        this.traderPosition = Objects.requireNonNull(traderPosition, "traderPosition");
        this.velX = velX;
        this.velY = velY;
        this.velZ = velZ;
        this.underAttack = underAttack;
        this.attackerRefId = attackerRefId;
        this.attackerApproxPosition = attackerApproxPosition != null ? attackerApproxPosition : UNKNOWN_ATTACKER_POSITION;
    }

    public String getCaravanId() {
        return caravanId;
    }

    public WorldPoint getTraderPosition() {
        return traderPosition;
    }

    public float getVelX() {
        return velX;
    }

    public float getVelY() {
        return velY;
    }

    public float getVelZ() {
        return velZ;
    }

    public boolean isUnderAttack() {
        return underAttack;
    }

    public int getAttackerRefId() {
        return attackerRefId;
    }

    public WorldPoint getAttackerApproxPosition() {
        return attackerApproxPosition;
    }
}
