package org.sokybot.town.api;

import java.util.Optional;

/**
 * Aggregate read model passed into logistics strategies for a single evaluation tick.
 */
public interface ITownSnapshot {

    String getMachineFullName();

    /** Monotonic-ish snapshot stamp; strategies may ignore or use for debouncing. */
    long getSnapshotEpochMillis();

    boolean isDead();

    Optional<Integer> getKillerEntityUniqueId();

    float getSelfX();

    float getSelfY();

    float getSelfZ();

    /** Region / map / layer id from the game model; {@code 0} if unknown. */
    int getRegionId();

    IInventorySnapshot getInventory();

    IDurabilitySnapshot getDurability();
}
