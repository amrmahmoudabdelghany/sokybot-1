package org.sokybot.behaviors.navigation;

/**
 * Machine-scoped settings for {@link SwarmLureCycleKeys#CYCLE_NAME} (scope {@code swarmLure}).
 */
public final class SwarmLureSettings {

    private boolean swarmLureCycleEnabled;

    /** Extra ms subtracted when computing {@code T_start} for converge (jitter / packet latency). */
    private long syncSafetyBufferMs = 200L;

    /** Horizontally projected travel speed for ETA (world units per second). */
    private float assumedWalkSpeedWorldPerSec = 12.0f;

    /** Horizontal distance within which fan-out / anchor approach counts as arrived. */
    private float arrivalRadiusWorld = 4.0f;

    public boolean isSwarmLureCycleEnabled() {
        return swarmLureCycleEnabled;
    }

    public void setSwarmLureCycleEnabled(boolean swarmLureCycleEnabled) {
        this.swarmLureCycleEnabled = swarmLureCycleEnabled;
    }

    public long getSyncSafetyBufferMs() {
        return syncSafetyBufferMs;
    }

    public void setSyncSafetyBufferMs(long syncSafetyBufferMs) {
        this.syncSafetyBufferMs = syncSafetyBufferMs;
    }

    public float getAssumedWalkSpeedWorldPerSec() {
        return assumedWalkSpeedWorldPerSec;
    }

    public void setAssumedWalkSpeedWorldPerSec(float assumedWalkSpeedWorldPerSec) {
        this.assumedWalkSpeedWorldPerSec = assumedWalkSpeedWorldPerSec;
    }

    public float getArrivalRadiusWorld() {
        return arrivalRadiusWorld;
    }

    public void setArrivalRadiusWorld(float arrivalRadiusWorld) {
        this.arrivalRadiusWorld = arrivalRadiusWorld;
    }
}
