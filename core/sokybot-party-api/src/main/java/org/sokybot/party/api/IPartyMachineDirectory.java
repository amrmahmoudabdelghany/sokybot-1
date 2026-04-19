package org.sokybot.party.api;

import java.util.Optional;

/**
 * Optional registry of each machine's character pose (fed by projections; used by coordinators / behaviors).
 */
public interface IPartyMachineDirectory {

    /**
     * Latest known entry for the machine full name ({@code group.machineName}), if loaded.
     */
    Optional<PartyMachineEntry> lookup(String machineFullName);
}
