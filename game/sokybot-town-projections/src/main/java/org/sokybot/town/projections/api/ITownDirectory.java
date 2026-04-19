package org.sokybot.town.projections.api;

import java.util.Optional;

import org.sokybot.town.api.NpcRef;

/**
 * Resolves world catalogue / navigation hints (vendors, town staging) for logistics planning.
 */
public interface ITownDirectory {

    Optional<NpcRef> nearestVendor(String machineFullName);

    /**
     * A town gate, stable, or recall anchor used to stage the return-to-hunt route.
     */
    Optional<NpcRef> nearestTown(String machineFullName);
}
