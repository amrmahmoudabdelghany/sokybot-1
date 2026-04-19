package org.sokybot.pet.projections.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sokybot.pet.api.IPetSnapshot;
import org.sokybot.pet.api.PetInfo;
import org.sokybot.pet.api.PetRole;

/**
 * Builds immutable {@link IPetSnapshot} views from {@link MachinePetState}.
 */
final class PetSnapshots {

    private PetSnapshots() {
    }

    static IPetSnapshot fromState(MachinePetState state, String machineFullName) {
        List<PetInfo> copy = new ArrayList<>(state.petsByEntityId.values());
        return new PetSnapshotImpl(machineFullName, Collections.unmodifiableList(copy), System.currentTimeMillis());
    }

    private static final class PetSnapshotImpl implements IPetSnapshot {

        private final String machineFullName;
        private final List<PetInfo> activePets;
        private final long capturedAtEpochMs;

        PetSnapshotImpl(String machineFullName, List<PetInfo> activePets, long capturedAtEpochMs) {
            this.machineFullName = machineFullName;
            this.activePets = activePets;
            this.capturedAtEpochMs = capturedAtEpochMs;
        }

        @Override
        public String getMachineFullName() {
            return machineFullName;
        }

        @Override
        public List<PetInfo> getActivePets() {
            return activePets;
        }

        @Override
        public Optional<PetInfo> getActivePetByRole(PetRole role) {
            if (role == null) {
                return Optional.empty();
            }
            for (PetInfo p : activePets) {
                if (p.getRole() == role && p.isAlive()) {
                    return Optional.of(p);
                }
            }
            return Optional.empty();
        }

        @Override
        public boolean hasActivePet(PetRole role) {
            return getActivePetByRole(role).isPresent();
        }

        @Override
        public long getCapturedAtEpochMs() {
            return capturedAtEpochMs;
        }
    }
}
