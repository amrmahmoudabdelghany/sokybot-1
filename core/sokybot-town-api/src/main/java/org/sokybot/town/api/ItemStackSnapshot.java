package org.sokybot.town.api;

import java.util.Objects;

/**
 * One occupied inventory slot in a snapshot.
 */
public final class ItemStackSnapshot {

    private final int slotIndex;
    private final int itemRefId;
    private final int quantity;
    /** {@code -1} when durability does not apply. */
    private final int durabilityPercent;

    private ItemStackSnapshot(Builder builder) {
        this.slotIndex = builder.slotIndex;
        this.itemRefId = builder.itemRefId;
        this.quantity = builder.quantity;
        this.durabilityPercent = builder.durabilityPercent;
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int slotIndex;
        private int itemRefId;
        private int quantity;
        private int durabilityPercent = -1;

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

        /**
         * @param durabilityPercent 0–100 inclusive, or -1 when N/A
         */
        public Builder durabilityPercent(int durabilityPercent) {
            this.durabilityPercent = durabilityPercent;
            return this;
        }

        public ItemStackSnapshot build() {
            return new ItemStackSnapshot(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemStackSnapshot)) {
            return false;
        }
        ItemStackSnapshot that = (ItemStackSnapshot) o;
        return slotIndex == that.slotIndex
                && itemRefId == that.itemRefId
                && quantity == that.quantity
                && durabilityPercent == that.durabilityPercent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slotIndex, itemRefId, quantity, durabilityPercent);
    }
}
