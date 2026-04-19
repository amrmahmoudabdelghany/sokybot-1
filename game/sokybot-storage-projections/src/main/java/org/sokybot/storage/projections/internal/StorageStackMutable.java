package org.sokybot.storage.projections.internal;

/**
 * Mutable slot occupant for warehouse projection; copied into immutable {@link org.sokybot.storage.api.StorageStack}.
 */
final class StorageStackMutable {

    final int slotIndex;
    int itemRefId;
    int quantity;
    /** 0–100 inclusive, or {@code -1} when unknown / not applicable. */
    int durabilityPercent = -1;
    long lastUpdateEpochMs;

    StorageStackMutable(int slotIndex, int itemRefId, int quantity, int durabilityPercent, long lastUpdateEpochMs) {
        this.slotIndex = slotIndex;
        this.itemRefId = itemRefId;
        this.quantity = quantity;
        this.durabilityPercent = durabilityPercent;
        this.lastUpdateEpochMs = lastUpdateEpochMs;
    }
}
