package org.sokybot.gameevents.events.storage;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Marker emitted once when the last storage data chunk for an open sequence completes.
 */
public final class StorageBoxFinalizeEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final byte storageType;
    private final int totalSlots;
    private final int filledSlots;

    public StorageBoxFinalizeEvent(String fullName, long timestamp, byte storageType, int totalSlots, int filledSlots) {
        this.fullName = fullName;
        this.timestamp = timestamp;
        this.storageType = storageType;
        this.totalSlots = totalSlots;
        this.filledSlots = filledSlots;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /** Mirrors {@link StorageOpenEvent#getStorageType()}. */
    public byte getStorageType() {
        return storageType;
    }

    /**
     * Declared warehouse capacity when available from the packet tail; {@code -1} when unknown.
     */
    public int getTotalSlots() {
        return totalSlots;
    }

    /**
     * Occupied slot count when available from the packet tail; {@code -1} when unknown.
     */
    public int getFilledSlots() {
        return filledSlots;
    }

    @Override
    public String toString() {
        return "StorageBoxFinalizeEvent{storageType=" + storageType + ", totalSlots=" + totalSlots + ", filledSlots="
                + filledSlots + "}";
    }
}
