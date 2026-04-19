package org.sokybot.town.projections.internal;

import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.town.api.EquipSlot;

/**
 * Mutable per-machine overlay for town logistics (inventory, durability, death, NPC dialog hints).
 */
final class MachineTownState {

    static final int DEFAULT_BACKPACK_CAPACITY = 108;

    volatile Integer selfEntityUniqueId;
    volatile long gold;
    volatile float selfX;
    volatile float selfY;
    volatile float selfZ;
    volatile int regionPack;
    volatile boolean dead;
    volatile Integer killerUniqueId;
    volatile long snapshotEpochMs;

    /** Backpack / inventory slots (includes equipped rows surfaced via the same opcode stream). */
    final ConcurrentHashMap<Integer, InvSlot> inventorySlots = new ConcurrentHashMap<>();

    /** Equipment durability percent 0–100 where known. */
    final ConcurrentHashMap<EquipSlot, Integer> equipDurabilityPercent = new ConcurrentHashMap<>();

    volatile int npcDialogNpcUniqueId;
    volatile boolean storageSessionOpen;

    MachineTownState() {
    }

    void touchEpoch() {
        this.snapshotEpochMs = System.currentTimeMillis();
    }

    static final class InvSlot {
        volatile int itemRefId;
        volatile int quantity;
        /** For inventory stacks where durability applies to the stack; {@code -1} if unknown. */
        volatile int durabilityPercent;

        InvSlot(int itemRefId, int quantity, int durabilityPercent) {
            this.itemRefId = itemRefId;
            this.quantity = quantity;
            this.durabilityPercent = durabilityPercent;
        }
    }
}
