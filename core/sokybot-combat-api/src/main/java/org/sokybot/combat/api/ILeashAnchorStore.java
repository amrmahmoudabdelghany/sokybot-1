package org.sokybot.combat.api;

import java.util.Optional;

/**
 * Stores the training-area anchor (world coordinates) per machine for leash enforcement.
 */
public interface ILeashAnchorStore {

    /**
     * Sets or replaces the leash anchor for the machine full name ({@code group.machine}).
     */
    void setAnchor(String machineFullName, float x, float y, float z);

    /**
     * Clears the anchor so leash distance falls back until set again.
     */
    void clearAnchor(String machineFullName);

    /**
     * Returns {@code [x, y, z]} world coordinates when configured.
     */
    Optional<float[]> getAnchor(String machineFullName);

}
