package org.sokybot.town.api;

import java.util.List;

/**
 * Read-only town loop settings mirrored from persisted user configuration.
 */
public interface ITownSettings {

    boolean isTownLoopEnabled();

    int getMinFreeInventorySlots();

    int getRepairDurabilityThresholdPercent();

    int getHpPotionItemRefId();

    int getMpPotionItemRefId();

    int getHpPotionTargetQuantity();

    int getMpPotionTargetQuantity();

    List<RestockItem> getExtraRestockTargets();

    long getBankGoldThreshold();

    boolean isStashOverflowLoot();

    boolean isWithdrawFromStorageEnabled();

    boolean isDepositOverflowToStorage();

    int getStorageOpenStaleAfterMs();

    boolean isTravelScriptEnabled();

    String getTravelScriptId();

    float getArrivalToleranceWorldUnits();

    long getLoadScreenStaleAfterMs();

    ITownPolicy toPolicy();
}
