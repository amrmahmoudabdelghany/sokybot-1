package org.sokybot.town.api;

import java.util.List;

/**
 * Derived policy snapshot from {@link ITownSettings} for pure strategy functions.
 */
public interface ITownPolicy {

    boolean isTownLoopEnabled();

    /** When free slots drop at or below this threshold, stash strategies may activate. */
    int getMinFreeInventorySlots();

    /** Repair when any observed durability percent is at or below this value (0–100). */
    int getRepairDurabilityThresholdPercent();

    int getHpPotionItemRefId();

    int getMpPotionItemRefId();

    /** Desired on-hand quantity for HP pots after a restock visit. */
    int getHpPotionTargetQuantity();

    /** Desired on-hand quantity for MP pots after a restock visit. */
    int getMpPotionTargetQuantity();

    /** User-defined multi-item restock rows (ammo, arrows, …). */
    List<RestockItem> getExtraRestockTargets();

    /** Gold amount above which banking / stash-of-gold behaviors may trigger. */
    long getBankGoldThreshold();

    /** When true, logistics may move non-essential loot into storage before hunting again. */
    boolean isStashOverflowLoot();
}
