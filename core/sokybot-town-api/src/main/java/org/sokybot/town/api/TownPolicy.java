package org.sokybot.town.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable default implementation of {@link ITownPolicy}.
 */
public final class TownPolicy implements ITownPolicy {

    private final boolean townLoopEnabled;
    private final int minFreeInventorySlots;
    private final int repairDurabilityThresholdPercent;
    private final int hpPotionItemRefId;
    private final int mpPotionItemRefId;
    private final int hpPotionTargetQuantity;
    private final int mpPotionTargetQuantity;
    private final List<RestockItem> extraRestockTargets;
    private final long bankGoldThreshold;
    private final boolean stashOverflowLoot;

    private TownPolicy(Builder builder) {
        this.townLoopEnabled = builder.townLoopEnabled;
        this.minFreeInventorySlots = builder.minFreeInventorySlots;
        this.repairDurabilityThresholdPercent = builder.repairDurabilityThresholdPercent;
        this.hpPotionItemRefId = builder.hpPotionItemRefId;
        this.mpPotionItemRefId = builder.mpPotionItemRefId;
        this.hpPotionTargetQuantity = builder.hpPotionTargetQuantity;
        this.mpPotionTargetQuantity = builder.mpPotionTargetQuantity;
        this.extraRestockTargets = Collections.unmodifiableList(new ArrayList<>(builder.extraRestockTargets));
        this.bankGoldThreshold = builder.bankGoldThreshold;
        this.stashOverflowLoot = builder.stashOverflowLoot;
    }

    public static TownPolicy copyOf(ITownPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        return TownPolicy.builder()
                .townLoopEnabled(policy.isTownLoopEnabled())
                .minFreeInventorySlots(policy.getMinFreeInventorySlots())
                .repairDurabilityThresholdPercent(policy.getRepairDurabilityThresholdPercent())
                .hpPotionItemRefId(policy.getHpPotionItemRefId())
                .mpPotionItemRefId(policy.getMpPotionItemRefId())
                .hpPotionTargetQuantity(policy.getHpPotionTargetQuantity())
                .mpPotionTargetQuantity(policy.getMpPotionTargetQuantity())
                .extraRestockTargets(policy.getExtraRestockTargets())
                .bankGoldThreshold(policy.getBankGoldThreshold())
                .stashOverflowLoot(policy.isStashOverflowLoot())
                .build();
    }

    @Override
    public boolean isTownLoopEnabled() {
        return townLoopEnabled;
    }

    @Override
    public int getMinFreeInventorySlots() {
        return minFreeInventorySlots;
    }

    @Override
    public int getRepairDurabilityThresholdPercent() {
        return repairDurabilityThresholdPercent;
    }

    @Override
    public int getHpPotionItemRefId() {
        return hpPotionItemRefId;
    }

    @Override
    public int getMpPotionItemRefId() {
        return mpPotionItemRefId;
    }

    @Override
    public int getHpPotionTargetQuantity() {
        return hpPotionTargetQuantity;
    }

    @Override
    public int getMpPotionTargetQuantity() {
        return mpPotionTargetQuantity;
    }

    @Override
    public List<RestockItem> getExtraRestockTargets() {
        return extraRestockTargets;
    }

    @Override
    public long getBankGoldThreshold() {
        return bankGoldThreshold;
    }

    @Override
    public boolean isStashOverflowLoot() {
        return stashOverflowLoot;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean townLoopEnabled = true;
        private int minFreeInventorySlots = 5;
        private int repairDurabilityThresholdPercent = 15;
        private int hpPotionItemRefId;
        private int mpPotionItemRefId;
        private int hpPotionTargetQuantity = 50;
        private int mpPotionTargetQuantity = 50;
        private final List<RestockItem> extraRestockTargets = new ArrayList<>();
        private long bankGoldThreshold = 1_000_000L;
        private boolean stashOverflowLoot = true;

        public Builder townLoopEnabled(boolean townLoopEnabled) {
            this.townLoopEnabled = townLoopEnabled;
            return this;
        }

        public Builder minFreeInventorySlots(int minFreeInventorySlots) {
            this.minFreeInventorySlots = minFreeInventorySlots;
            return this;
        }

        public Builder repairDurabilityThresholdPercent(int repairDurabilityThresholdPercent) {
            this.repairDurabilityThresholdPercent = repairDurabilityThresholdPercent;
            return this;
        }

        public Builder hpPotionItemRefId(int hpPotionItemRefId) {
            this.hpPotionItemRefId = hpPotionItemRefId;
            return this;
        }

        public Builder mpPotionItemRefId(int mpPotionItemRefId) {
            this.mpPotionItemRefId = mpPotionItemRefId;
            return this;
        }

        public Builder hpPotionTargetQuantity(int hpPotionTargetQuantity) {
            this.hpPotionTargetQuantity = hpPotionTargetQuantity;
            return this;
        }

        public Builder mpPotionTargetQuantity(int mpPotionTargetQuantity) {
            this.mpPotionTargetQuantity = mpPotionTargetQuantity;
            return this;
        }

        public Builder addExtraRestockTarget(RestockItem item) {
            if (item != null) {
                this.extraRestockTargets.add(item);
            }
            return this;
        }

        public Builder extraRestockTargets(List<RestockItem> items) {
            this.extraRestockTargets.clear();
            if (items != null) {
                this.extraRestockTargets.addAll(items);
            }
            return this;
        }

        public Builder bankGoldThreshold(long bankGoldThreshold) {
            this.bankGoldThreshold = bankGoldThreshold;
            return this;
        }

        public Builder stashOverflowLoot(boolean stashOverflowLoot) {
            this.stashOverflowLoot = stashOverflowLoot;
            return this;
        }

        public TownPolicy build() {
            return new TownPolicy(this);
        }
    }
}
