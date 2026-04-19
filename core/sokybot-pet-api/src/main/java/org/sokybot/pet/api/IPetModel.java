package org.sokybot.pet.api;

import java.util.Optional;

/**
 * OSGi service: query active pets for a machine from the pet projection.
 */
public interface IPetModel {

    Optional<IPetSnapshot> snapshot(String machineFullName);

    Optional<PetInfo> findActiveByRole(String machineFullName, PetRole role);

    boolean isPetActive(String machineFullName, PetRole role);
}
