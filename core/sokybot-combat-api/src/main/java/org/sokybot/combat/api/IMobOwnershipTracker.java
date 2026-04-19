package org.sokybot.combat.api;

import java.util.Optional;

/**
 * Tracks first damaging caster per monster entity for kill-stealing avoidance (skill-based attribution).
 */
public interface IMobOwnershipTracker {

    /**
     * Entity id of the player who initiated combat on this monster via a skill cast first seen by this tracker,
     * when known; empty when no skill hit has been observed yet for this spawn.
     */
    Optional<Integer> getFirstAttackerEntityId(String machineFullName, int monsterEntityId);

    /**
     * Removes bookkeeping for a monster entity (e.g. despawn).
     */
    void forgetMonster(String machineFullName, int monsterEntityId);

}
