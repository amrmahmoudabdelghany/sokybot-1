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
    private final boolean withdrawFromStorageEnabled;
    private final boolean depositOverflowToStorage;
    private final int storageOpenStaleAfterMs;
    private final boolean travelScriptEnabled;
    private final String travelScriptId;
    private final float arrivalToleranceWorldUnits;
    private final long loadScreenStaleAfterMs;

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
        this.withdrawFromStorageEnabled = builder.withdrawFromStorageEnabled;
        this.depositOverflowToStorage = builder.depositOverflowToStorage;
        this.storageOpenStaleAfterMs = builder.storageOpenStaleAfterMs;
        this.travelScriptEnabled = builder.travelScriptEnabled;
        this.travelScriptId = builder.travelScriptId;
        this.arrivalToleranceWorldUnits = builder.arrivalToleranceWorldUnits;
        this.loadScreenStaleAfterMs = builder.loadScreenStaleAfterMs;
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
                .withdrawFromStorageEnabled(policy.isWithdrawFromStorageEnabled())
                .depositOverflowToStorage(policy.isDepositOverflowToStorage())
                .storageOpenStaleAfterMs(policy.getStorageOpenStaleAfterMs())
                .travelScriptEnabled(policy.isTravelScriptEnabled())
                .travelScriptId(policy.getTravelScriptId())
                .arrivalToleranceWorldUnits(policy.getArrivalToleranceWorldUnits())
                .loadScreenStaleAfterMs(policy.getLoadScreenStaleAfterMs())
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

    @Override
    public boolean isWithdrawFromStorageEnabled() {
        return withdrawFromStorageEnabled;
    }

    @Override
    public boolean isDepositOverflowToStorage() {
        return depositOverflowToStorage;
    }

    @Override
    public int getStorageOpenStaleAfterMs() {
        return storageOpenStaleAfterMs;
    }

    @Override
    public boolean isTravelScriptEnabled() {
        return travelScriptEnabled;
    }

    @Override
    public String getTravelScriptId() {
        return travelScriptId;
    }

    @Override
    public float getArrivalToleranceWorldUnits() {
        return arrivalToleranceWorldUnits;
    }

    @Override
    public long getLoadScreenStaleAfterMs() {
        return loadScreenStaleAfterMs;
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
        private boolean withdrawFromStorageEnabled;
        private boolean depositOverflowToStorage;
        private int storageOpenStaleAfterMs = 30_000;
        private boolean travelScriptEnabled;
        private String travelScriptId;
        private float arrivalToleranceWorldUnits = 1.5f;
        private long loadScreenStaleAfterMs = 30_000L;

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

        public Builder withdrawFromStorageEnabled(boolean withdrawFromStorageEnabled) {
            this.withdrawFromStorageEnabled = withdrawFromStorageEnabled;
            return this;
        }

        public Builder depositOverflowToStorage(boolean depositOverflowToStorage) {
            this.depositOverflowToStorage = depositOverflowToStorage;
            return this;
        }

        public Builder storageOpenStaleAfterMs(int storageOpenStaleAfterMs) {
            this.storageOpenStaleAfterMs = storageOpenStaleAfterMs;
            return this;
        }

        public Builder travelScriptEnabled(boolean travelScriptEnabled) {
            this.travelScriptEnabled = travelScriptEnabled;
            return this;
        }

        public Builder travelScriptId(String travelScriptId) {
            this.travelScriptId = travelScriptId;
            return this;
        }

        public Builder arrivalToleranceWorldUnits(float arrivalToleranceWorldUnits) {
            this.arrivalToleranceWorldUnits = arrivalToleranceWorldUnits;
            return this;
        }

        public Builder loadScreenStaleAfterMs(long loadScreenStaleAfterMs) {
            this.loadScreenStaleAfterMs = loadScreenStaleAfterMs;
            return this;
        }

        public TownPolicy build() {
            return new TownPolicy(this);
        }
    }
}
