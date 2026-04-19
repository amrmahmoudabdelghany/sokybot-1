package org.sokybot.town.api;

import java.util.Objects;

/**
 * Durability observation for one equipment slot (repair planning).
 */
public final class EquipDurability {

    private final EquipSlot slot;
    /** 0–100 inclusive. */
    private final int durabilityPercent;

    private EquipDurability(Builder builder) {
        this.slot = Objects.requireNonNull(builder.slot, "slot");
        this.durabilityPercent = builder.durabilityPercent;
    }

    public EquipSlot getSlot() {
        return slot;
    }

    public int getDurabilityPercent() {
        return durabilityPercent;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private EquipSlot slot;
        private int durabilityPercent;

        public Builder slot(EquipSlot slot) {
            this.slot = slot;
            return this;
        }

        public Builder durabilityPercent(int durabilityPercent) {
            this.durabilityPercent = durabilityPercent;
            return this;
        }

        public EquipDurability build() {
            return new EquipDurability(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EquipDurability)) {
            return false;
        }
        EquipDurability that = (EquipDurability) o;
        return durabilityPercent == that.durabilityPercent && slot == that.slot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot, durabilityPercent);
    }
}
