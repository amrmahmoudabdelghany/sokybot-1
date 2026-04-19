package org.sokybot.town.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Planned inventory moves into storage (bank chest, guild stash, …).
 */
public final class StashOrder {

    private final List<Integer> inventorySlotIndexes;
    private final VendorRef preferredStorageNpc;

    private StashOrder(Builder builder) {
        this.inventorySlotIndexes = Collections.unmodifiableList(new ArrayList<>(builder.inventorySlotIndexes));
        this.preferredStorageNpc = builder.preferredStorageNpc;
    }

    public List<Integer> getInventorySlotIndexes() {
        return inventorySlotIndexes;
    }

    public Optional<VendorRef> getPreferredStorageNpc() {
        return Optional.ofNullable(preferredStorageNpc);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Integer> inventorySlotIndexes = new ArrayList<>();
        private VendorRef preferredStorageNpc;

        public Builder addSlotIndex(int slotIndex) {
            this.inventorySlotIndexes.add(slotIndex);
            return this;
        }

        public Builder slotIndexes(List<Integer> slotIndexes) {
            this.inventorySlotIndexes.clear();
            if (slotIndexes != null) {
                this.inventorySlotIndexes.addAll(slotIndexes);
            }
            return this;
        }

        public Builder preferredStorageNpc(VendorRef preferredStorageNpc) {
            this.preferredStorageNpc = preferredStorageNpc;
            return this;
        }

        public StashOrder build() {
            return new StashOrder(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StashOrder)) {
            return false;
        }
        StashOrder that = (StashOrder) o;
        return inventorySlotIndexes.equals(that.inventorySlotIndexes)
                && Objects.equals(preferredStorageNpc, that.preferredStorageNpc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inventorySlotIndexes, preferredStorageNpc);
    }
}
