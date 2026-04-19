package org.sokybot.storage.projections.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.sokybot.storage.api.IGuildStorageSnapshot;
import org.sokybot.storage.api.IStorageSnapshot;
import org.sokybot.storage.api.StorageStack;
import org.sokybot.storage.api.StorageType;

/**
 * Builds immutable {@link IStorageSnapshot} / {@link IGuildStorageSnapshot} views from mutable machine state.
 */
final class StorageSnapshots {

    private StorageSnapshots() {
    }

    static IStorageSnapshot buildPersonal(String machineFullName, MachineStorageState st) {
        return new PersonalSnapshot(machineFullName, st);
    }

    static IGuildStorageSnapshot buildGuild(String machineFullName, MachineStorageState st) {
        return new GuildSnapshot(machineFullName, st);
    }

    private static List<StorageStack> stacksFrom(ConcurrentMapView view) {
        List<StorageStack> list = new ArrayList<>();
        for (Integer idx : new TreeSet<>(view.keySet())) {
            StorageStackMutable m = view.get(idx);
            if (m == null || m.quantity <= 0 || m.itemRefId == 0) {
                continue;
            }
            list.add(StorageStack.builder()
                    .slotIndex(m.slotIndex)
                    .itemRefId(m.itemRefId)
                    .quantity(m.quantity)
                    .durabilityPercent(m.durabilityPercent)
                    .lastUpdateEpochMs(m.lastUpdateEpochMs)
                    .build());
        }
        return list;
    }

    /** Narrow view of concurrent map keys for sorting without exposing generic in API. */
    private interface ConcurrentMapView {
        java.util.Set<Integer> keySet();

        StorageStackMutable get(Integer k);
    }

    private static final class PersonalSnapshot implements IStorageSnapshot {

        private final String machineFullName;
        private final MachineStorageState st;

        PersonalSnapshot(String machineFullName, MachineStorageState st) {
            this.machineFullName = machineFullName;
            this.st = st;
        }

        @Override
        public String getMachineFullName() {
            return machineFullName;
        }

        @Override
        public StorageType getStorageType() {
            return StorageType.PERSONAL;
        }

        @Override
        public long getCapturedAtEpochMs() {
            return st.capturedAtEpochMs;
        }

        @Override
        public long getStorageGold() {
            return st.personalGold;
        }

        @Override
        public int getTotalSlots() {
            return st.personalTotalSlots;
        }

        @Override
        public List<StorageStack> getStacks() {
            return stacksFrom(new ConcurrentMapView() {
                @Override
                public java.util.Set<Integer> keySet() {
                    return st.personalStacks.keySet();
                }

                @Override
                public StorageStackMutable get(Integer k) {
                    return st.personalStacks.get(k);
                }
            });
        }

        @Override
        public boolean isFresh() {
            return st.personalFresh;
        }
    }

    private static final class GuildSnapshot implements IGuildStorageSnapshot {

        private final String machineFullName;
        private final MachineStorageState st;

        GuildSnapshot(String machineFullName, MachineStorageState st) {
            this.machineFullName = machineFullName;
            this.st = st;
        }

        @Override
        public String getMachineFullName() {
            return machineFullName;
        }

        @Override
        public StorageType getStorageType() {
            return StorageType.GUILD;
        }

        @Override
        public long getCapturedAtEpochMs() {
            return st.capturedAtEpochMs;
        }

        @Override
        public long getStorageGold() {
            return st.guildGold;
        }

        @Override
        public int getTotalSlots() {
            return st.guildTotalSlots;
        }

        @Override
        public List<StorageStack> getStacks() {
            return stacksFrom(new ConcurrentMapView() {
                @Override
                public java.util.Set<Integer> keySet() {
                    return st.guildStacks.keySet();
                }

                @Override
                public StorageStackMutable get(Integer k) {
                    return st.guildStacks.get(k);
                }
            });
        }

        @Override
        public boolean isFresh() {
            return st.guildFresh;
        }
    }
}
