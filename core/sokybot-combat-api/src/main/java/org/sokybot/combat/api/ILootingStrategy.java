package org.sokybot.combat.api;

import java.util.Optional;

/**
 * Chooses which dropped item to pick up next.
 */
public interface ILootingStrategy {

    Optional<DroppedItemRef> pickNext(ICombatSnapshot snapshot, ICombatPolicy policy);
}
