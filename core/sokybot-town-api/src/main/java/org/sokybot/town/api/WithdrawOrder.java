package org.sokybot.town.api;

import java.util.Objects;
import java.util.Optional;

/**
 * Planned withdraw from personal storage into inventory (one slot batch).
 */
public final class WithdrawOrder {

    private final int storageSlotIndex;
    private final int quantity;
    private final int targetItemRefId;
    private final VendorRef preferredStorageNpc;

    private WithdrawOrder(Builder builder) {
        this.storageSlotIndex = builder.storageSlotIndex;
        this.quantity = builder.quantity;
        this.targetItemRefId = builder.targetItemRefId;
        this.preferredStorageNpc = builder.preferredStorageNpc;
    }

    public int getStorageSlotIndex() {
        return storageSlotIndex;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getTargetItemRefId() {
        return targetItemRefId;
    }

    public Optional<VendorRef> getPreferredStorageNpc() {
        return Optional.ofNullable(preferredStorageNpc);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int storageSlotIndex;
        private int quantity;
        private int targetItemRefId;
        private VendorRef preferredStorageNpc;

        public Builder storageSlotIndex(int storageSlotIndex) {
            this.storageSlotIndex = storageSlotIndex;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder targetItemRefId(int targetItemRefId) {
            this.targetItemRefId = targetItemRefId;
            return this;
        }

        public Builder preferredStorageNpc(VendorRef preferredStorageNpc) {
            this.preferredStorageNpc = preferredStorageNpc;
            return this;
        }

        public WithdrawOrder build() {
            return new WithdrawOrder(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WithdrawOrder)) {
            return false;
        }
        WithdrawOrder that = (WithdrawOrder) o;
        return storageSlotIndex == that.storageSlotIndex
                && quantity == that.quantity
                && targetItemRefId == that.targetItemRefId
                && Objects.equals(preferredStorageNpc, that.preferredStorageNpc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storageSlotIndex, quantity, targetItemRefId, preferredStorageNpc);
    }
}
