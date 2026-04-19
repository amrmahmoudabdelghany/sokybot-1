package org.sokybot.navigation.api;

/**
 * Tracks recent movement samples per machine and reports whether the character appears stuck.
 */
public interface IStuckDetector {

    /**
     * Records an observed position sample (typically from movement events).
     */
    void recordPosition(String machineFullName, float x, float y, float z, long epochMillis);

    /** True when recent path length within the sliding window falls below the stuck threshold. */
    boolean isStuck(String machineFullName);

    /** Clears recorded samples for the machine (e.g. after issuing an unstuck walk). */
    void reset(String machineFullName);
}
