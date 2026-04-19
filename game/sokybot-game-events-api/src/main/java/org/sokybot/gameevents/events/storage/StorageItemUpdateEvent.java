package org.sokybot.gameevents.events.storage;

import java.util.Arrays;

import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Emitted when a storage slot row is decoded from a storage data chunk (e.g. opcode 0x3049).
 */
public final class StorageItemUpdateEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final byte storageType;
    private final int slotIndex;
    private final int itemRefId;
    private final int quantity;
    /** 0–100 inclusive, or {@code -1} when not applicable. */
    private final int durabilityPercent;
    private final byte[] rawTail;

    public StorageItemUpdateEvent(String fullName, long timestamp, byte storageType, int slotIndex, int itemRefId,
            int quantity, int durabilityPercent, byte[] rawTail) {
        this.fullName = fullName;
        this.timestamp = timestamp;
        this.storageType = storageType;
        this.slotIndex = slotIndex;
        this.itemRefId = itemRefId;
        this.quantity = quantity;
        this.durabilityPercent = durabilityPercent;
        this.rawTail = rawTail != null ? Arrays.copyOf(rawTail, rawTail.length) : new byte[0];
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /** Mirrors {@link StorageOpenEvent#getStorageType()} ({@link StorageOpenEvent#TYPE_PERSONAL} / {@link StorageOpenEvent#TYPE_GUILD}). */
    public byte getStorageType() {
        return storageType;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public int getItemRefId() {
        return itemRefId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getDurabilityPercent() {
        return durabilityPercent;
    }

    /** Opaque trailing bytes from the parser for forward-compatible flags. */
    public byte[] getRawTail() {
        return Arrays.copyOf(rawTail, rawTail.length);
    }

    @Override
    public String toString() {
        return "StorageItemUpdateEvent{storageType=" + storageType + ", slotIndex=" + slotIndex + ", itemRefId="
                + itemRefId + ", quantity=" + quantity + ", durabilityPercent=" + durabilityPercent + "}";
    }
}
