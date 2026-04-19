package org.sokybot.town.api;

import java.util.Optional;

/**
 * Equipment durability projection used by repair strategies.
 */
public interface IDurabilitySnapshot {

    /**
     * Durability percent 0–100, or empty when no item is equipped in that slot.
     */
    Optional<Integer> getDurabilityPercent(EquipSlot slot);
}
