package org.sokybot.storage.api;

import java.util.List;
import java.util.Optional;

/**
 * Immutable read-only view of one machine's storage box for a given {@link StorageType}.
 */
public interface IStorageSnapshot {

    String getMachineFullName();

    StorageType getStorageType();

    long getCapturedAtEpochMs();

    long getStorageGold();

    int getTotalSlots();

    List<StorageStack> getStacks();

    default Optional<StorageStack> findSlot(int slotIndex) {
        for (StorageStack s : getStacks()) {
            if (s.getSlotIndex() == slotIndex) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    default int countItemRef(int itemRefId) {
        int n = 0;
        for (StorageStack s : getStacks()) {
            if (s.getItemRefId() == itemRefId) {
                n += s.getQuantity();
            }
        }
        return n;
    }

    /** True once a full snapshot has been bound for the current open window. */
    boolean isFresh();
}
