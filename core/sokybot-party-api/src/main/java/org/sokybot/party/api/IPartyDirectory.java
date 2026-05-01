package org.sokybot.party.api;

import java.util.Optional;

/**
 * Resolves cross-machine party identity links (leader/follower machine mapping).
 */
public interface IPartyDirectory {

    /**
     * Resolves leader machine full name for a follower machine (group.machine), if known.
     */
    Optional<String> resolveLeaderMachine(String followerMachineFullName);
}
