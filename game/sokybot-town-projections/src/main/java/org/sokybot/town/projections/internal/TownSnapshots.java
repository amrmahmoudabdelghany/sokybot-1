package org.sokybot.town.projections.internal;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import org.sokybot.town.api.IDurabilitySnapshot;
import org.sokybot.town.api.IInventorySnapshot;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.ItemStackSnapshot;
import org.sokybot.town.api.EquipSlot;

final class TownSnapshots {

    private TownSnapshots() {
    }

    static ITownSnapshot build(String machineFullName, MachineTownState st) {
        List<ItemStackSnapshot> stacks = new ArrayList<>();
        int occupied = 0;
        for (var e : st.inventorySlots.entrySet()) {
            MachineTownState.InvSlot s = e.getValue();
            if (s.quantity > 0 && s.itemRefId != 0) {
                occupied++;
                stacks.add(ItemStackSnapshot.builder()
                        .slotIndex(e.getKey())
                        .itemRefId(s.itemRefId)
                        .quantity(s.quantity)
                        .durabilityPercent(s.durabilityPercent)
                        .build());
            }
        }
        int total = MachineTownState.DEFAULT_BACKPACK_CAPACITY;
        int free = Math.max(0, total - occupied);
        IInventorySnapshot inv = new InventorySnapshotImpl(st.gold, total, free, List.copyOf(stacks));
        EnumMap<EquipSlot, Integer> durCopy = new EnumMap<>(EquipSlot.class);
        durCopy.putAll(st.equipDurabilityPercent);
        IDurabilitySnapshot dur = new DurabilitySnapshotImpl(durCopy);
        return new TownSnapshotImpl(machineFullName, st.snapshotEpochMs, st.dead, st.killerUniqueId,
                st.selfX, st.selfY, st.selfZ, st.regionPack, inv, dur);
    }

    private static final class TownSnapshotImpl implements ITownSnapshot {

        private final String machineFullName;
        private final long snapshotEpochMs;
        private final boolean dead;
        private final Integer killerUniqueId;
        private final float selfX;
        private final float selfY;
        private final float selfZ;
        private final int regionId;
        private final IInventorySnapshot inventory;
        private final IDurabilitySnapshot durability;

        TownSnapshotImpl(String machineFullName, long snapshotEpochMs, boolean dead, Integer killerUniqueId,
                float selfX, float selfY, float selfZ, int regionId,
                IInventorySnapshot inventory, IDurabilitySnapshot durability) {
            this.machineFullName = machineFullName;
            this.snapshotEpochMs = snapshotEpochMs;
            this.dead = dead;
            this.killerUniqueId = killerUniqueId;
            this.selfX = selfX;
            this.selfY = selfY;
            this.selfZ = selfZ;
            this.regionId = regionId;
            this.inventory = inventory;
            this.durability = durability;
        }

        @Override
        public String getMachineFullName() {
            return machineFullName;
        }

        @Override
        public long getSnapshotEpochMillis() {
            return snapshotEpochMs;
        }

        @Override
        public boolean isDead() {
            return dead;
        }

        @Override
        public Optional<Integer> getKillerEntityUniqueId() {
            return Optional.ofNullable(killerUniqueId);
        }

        @Override
        public float getSelfX() {
            return selfX;
        }

        @Override
        public float getSelfY() {
            return selfY;
        }

        @Override
        public float getSelfZ() {
            return selfZ;
        }

        @Override
        public int getRegionId() {
            return regionId;
        }

        @Override
        public IInventorySnapshot getInventory() {
            return inventory;
        }

        @Override
        public IDurabilitySnapshot getDurability() {
            return durability;
        }
    }

    private static final class InventorySnapshotImpl implements IInventorySnapshot {

        private final long gold;
        private final int totalSlots;
        private final int freeSlots;
        private final List<ItemStackSnapshot> stacks;

        InventorySnapshotImpl(long gold, int totalSlots, int freeSlots, List<ItemStackSnapshot> stacks) {
            this.gold = gold;
            this.totalSlots = totalSlots;
            this.freeSlots = freeSlots;
            this.stacks = stacks;
        }

        @Override
        public long getGold() {
            return gold;
        }

        @Override
        public int getFreeSlots() {
            return freeSlots;
        }

        @Override
        public int getTotalSlots() {
            return totalSlots;
        }

        @Override
        public int countItemRef(int itemRefId) {
            int sum = 0;
            for (ItemStackSnapshot s : stacks) {
                if (s.getItemRefId() == itemRefId) {
                    sum += s.getQuantity();
                }
            }
            return sum;
        }

        @Override
        public List<ItemStackSnapshot> listStacks() {
            return stacks;
        }

        @Override
        public Optional<ItemStackSnapshot> findSlot(int slotIndex) {
            for (ItemStackSnapshot s : stacks) {
                if (s.getSlotIndex() == slotIndex) {
                    return Optional.of(s);
                }
            }
            return Optional.empty();
        }
    }

    private static final class DurabilitySnapshotImpl implements IDurabilitySnapshot {

        private final EnumMap<EquipSlot, Integer> map;

        DurabilitySnapshotImpl(EnumMap<EquipSlot, Integer> map) {
            this.map = map;
        }

        @Override
        public Optional<Integer> getDurabilityPercent(EquipSlot slot) {
            return Optional.ofNullable(map.get(slot));
        }
    }
}
