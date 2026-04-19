package org.sokybot.pet.api;

import java.util.List;
import java.util.Optional;

/**
 * Read-only per-machine view of tracked pets from the projection.
 */
public interface IPetSnapshot {

    String getMachineFullName();

    List<PetInfo> getActivePets();

    Optional<PetInfo> getActivePetByRole(PetRole role);

    boolean hasActivePet(PetRole role);

    long getCapturedAtEpochMs();
}
