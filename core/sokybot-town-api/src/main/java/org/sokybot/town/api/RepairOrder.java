package org.sokybot.town.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Planned repair operations for the next blacksmith visit.
 */
public final class RepairOrder {

    private final List<EquipDurability> slots;
    private final VendorRef preferredVendor;

    private RepairOrder(Builder builder) {
        this.slots = Collections.unmodifiableList(new ArrayList<>(builder.slots));
        this.preferredVendor = builder.preferredVendor;
    }

    public List<EquipDurability> getSlots() {
        return slots;
    }

    public Optional<VendorRef> getPreferredVendor() {
        return Optional.ofNullable(preferredVendor);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<EquipDurability> slots = new ArrayList<>();
        private VendorRef preferredVendor;

        public Builder addSlot(EquipDurability slot) {
            if (slot != null) {
                this.slots.add(slot);
            }
            return this;
        }

        public Builder slots(List<EquipDurability> slots) {
            this.slots.clear();
            if (slots != null) {
                this.slots.addAll(slots);
            }
            return this;
        }

        public Builder preferredVendor(VendorRef preferredVendor) {
            this.preferredVendor = preferredVendor;
            return this;
        }

        public RepairOrder build() {
            return new RepairOrder(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RepairOrder)) {
            return false;
        }
        RepairOrder that = (RepairOrder) o;
        return slots.equals(that.slots) && Objects.equals(preferredVendor, that.preferredVendor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slots, preferredVendor);
    }
}
