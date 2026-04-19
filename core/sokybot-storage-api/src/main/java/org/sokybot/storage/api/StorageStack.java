package org.sokybot.storage.api;

import java.util.Objects;

/**
 * Immutable occupant of one storage slot.
 */
public final class StorageStack {

    private final int slotIndex;
    private final int itemRefId;
    private final int quantity;
    /** 0–100 inclusive, or {@code -1} when durability does not apply. */
    private final int durabilityPercent;
    private final long lastUpdateEpochMs;

    private StorageStack(Builder builder) {
        this.slotIndex = builder.slotIndex;
        this.itemRefId = builder.itemRefId;
        this.quantity = builder.quantity;
        this.durabilityPercent = builder.durabilityPercent;
        this.lastUpdateEpochMs = builder.lastUpdateEpochMs;
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

    public long getLastUpdateEpochMs() {
        return lastUpdateEpochMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int slotIndex;
        private int itemRefId;
        private int quantity;
        private int durabilityPercent = -1;
        private long lastUpdateEpochMs;

        public Builder slotIndex(int slotIndex) {
            this.slotIndex = slotIndex;
            return this;
        }

        public Builder itemRefId(int itemRefId) {
            this.itemRefId = itemRefId;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder durabilityPercent(int durabilityPercent) {
            this.durabilityPercent = durabilityPercent;
            return this;
        }

        public Builder lastUpdateEpochMs(long lastUpdateEpochMs) {
            this.lastUpdateEpochMs = lastUpdateEpochMs;
            return this;
        }

        public StorageStack build() {
            return new StorageStack(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StorageStack)) {
            return false;
        }
        StorageStack that = (StorageStack) o;
        return slotIndex == that.slotIndex && itemRefId == that.itemRefId && quantity == that.quantity
                && durabilityPercent == that.durabilityPercent && lastUpdateEpochMs == that.lastUpdateEpochMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotIndex, itemRefId, quantity, durabilityPercent, lastUpdateEpochMs);
    }
}
