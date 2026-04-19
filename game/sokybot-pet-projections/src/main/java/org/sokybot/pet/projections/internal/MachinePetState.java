package org.sokybot.pet.projections.internal;

import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.pet.api.PetInfo;

/**
 * Mutable per-machine projection state (internal).
 */
final class MachinePetState {

    /** Player unique id from {@link org.sokybot.gameevents.events.character.CharacterLoadedEvent}; 0 if unknown. */
    volatile int ownerUniqueId;

    /** Active pets keyed by COS entity unique id. */
    final ConcurrentHashMap<Integer, PetInfo> petsByEntityId = new ConcurrentHashMap<>();
}
