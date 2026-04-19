package org.sokybot.town.api;

import java.util.List;
import java.util.Optional;

/**
 * Read-only projection of backpack / consumable inventory for logistics strategies.
 */
public interface IInventorySnapshot {

    long getGold();

    int getFreeSlots();

    int getTotalSlots();

    /**
     * Total quantity across all stacks for the given catalogue ref id.
     */
    int countItemRef(int itemRefId);

    List<ItemStackSnapshot> listStacks();

    Optional<ItemStackSnapshot> findSlot(int slotIndex);
}
