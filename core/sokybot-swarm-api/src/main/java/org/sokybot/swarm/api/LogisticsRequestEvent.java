package org.sokybot.swarm.api;

import java.util.Objects;

/**
 * Farmer-originated logistics help request (claim key = {@link #getRequestId()}).
 */
public final class LogisticsRequestEvent extends SwarmEvent {

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        EMERGENCY
    }

    private final int worldX;
    private final int worldZ;
    private final int worldY;
    private final int regionId;
    private final Priority priority;
    private final int freeSlotsRemaining;
    private final long farmerSpawnId;
    private final long expiresAtEpochMs;

    public LogisticsRequestEvent(
            String requesterMachineId,
            long timestampEpochMs,
            String requestId,
            int worldX,
            int worldZ,
            int worldY,
            int regionId,
            Priority priority,
            int freeSlotsRemaining,
            long farmerSpawnId,
            long expiresAtEpochMs) {
        super(requesterMachineId, timestampEpochMs, requestId);
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.worldY = worldY;
        this.regionId = regionId;
        this.priority = Objects.requireNonNullElse(priority, Priority.NORMAL);
        this.freeSlotsRemaining = freeSlotsRemaining;
        this.farmerSpawnId = farmerSpawnId;
        this.expiresAtEpochMs = expiresAtEpochMs;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldZ() {
        return worldZ;
    }

    public int getWorldY() {
        return worldY;
    }

    public int getRegionId() {
        return regionId;
    }

    public Priority getPriority() {
        return priority;
    }

    public int getFreeSlotsRemaining() {
        return freeSlotsRemaining;
    }

    public long getFarmerSpawnId() {
        return farmerSpawnId;
    }

    public long getExpiresAtEpochMs() {
        return expiresAtEpochMs;
    }
}
