package org.sokybot.behaviors.town.internal.settings;

import java.util.ArrayList;
import java.util.List;

import org.sokybot.town.api.ITownPolicy;
import org.sokybot.town.api.ITownSettings;
import org.sokybot.town.api.RestockItem;
import org.sokybot.town.api.TownPolicy;

/**
 * Machine-scoped town settings (scope {@code town}).
 */
public final class TownSettings implements ITownSettings {

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

    @Override
    public boolean isTownLoopEnabled() {
        return townLoopEnabled;
    }

    public void setTownLoopEnabled(boolean townLoopEnabled) {
        this.townLoopEnabled = townLoopEnabled;
    }

    @Override
    public int getMinFreeInventorySlots() {
        return minFreeInventorySlots;
    }

    public void setMinFreeInventorySlots(int minFreeInventorySlots) {
        this.minFreeInventorySlots = minFreeInventorySlots;
    }

    @Override
    public int getRepairDurabilityThresholdPercent() {
        return repairDurabilityThresholdPercent;
    }

    public void setRepairDurabilityThresholdPercent(int repairDurabilityThresholdPercent) {
        this.repairDurabilityThresholdPercent = repairDurabilityThresholdPercent;
    }

    @Override
    public int getHpPotionItemRefId() {
        return hpPotionItemRefId;
    }

    public void setHpPotionItemRefId(int hpPotionItemRefId) {
        this.hpPotionItemRefId = hpPotionItemRefId;
    }

    @Override
    public int getMpPotionItemRefId() {
        return mpPotionItemRefId;
    }

    public void setMpPotionItemRefId(int mpPotionItemRefId) {
        this.mpPotionItemRefId = mpPotionItemRefId;
    }

    @Override
    public int getHpPotionTargetQuantity() {
        return hpPotionTargetQuantity;
    }

    public void setHpPotionTargetQuantity(int hpPotionTargetQuantity) {
        this.hpPotionTargetQuantity = hpPotionTargetQuantity;
    }

    @Override
    public int getMpPotionTargetQuantity() {
        return mpPotionTargetQuantity;
    }

    public void setMpPotionTargetQuantity(int mpPotionTargetQuantity) {
        this.mpPotionTargetQuantity = mpPotionTargetQuantity;
    }

    @Override
    public List<RestockItem> getExtraRestockTargets() {
        return extraRestockTargets;
    }

    @Override
    public long getBankGoldThreshold() {
        return bankGoldThreshold;
    }

    public void setBankGoldThreshold(long bankGoldThreshold) {
        this.bankGoldThreshold = bankGoldThreshold;
    }

    @Override
    public boolean isStashOverflowLoot() {
        return stashOverflowLoot;
    }

    public void setStashOverflowLoot(boolean stashOverflowLoot) {
        this.stashOverflowLoot = stashOverflowLoot;
    }

    @Override
    public ITownPolicy toPolicy() {
        return TownPolicy.builder()
                .townLoopEnabled(townLoopEnabled)
                .minFreeInventorySlots(minFreeInventorySlots)
                .repairDurabilityThresholdPercent(repairDurabilityThresholdPercent)
                .hpPotionItemRefId(hpPotionItemRefId)
                .mpPotionItemRefId(mpPotionItemRefId)
                .hpPotionTargetQuantity(hpPotionTargetQuantity)
                .mpPotionTargetQuantity(mpPotionTargetQuantity)
                .extraRestockTargets(extraRestockTargets)
                .bankGoldThreshold(bankGoldThreshold)
                .stashOverflowLoot(stashOverflowLoot)
                .build();
    }
}
