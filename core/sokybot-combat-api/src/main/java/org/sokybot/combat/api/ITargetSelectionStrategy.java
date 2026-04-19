package org.sokybot.combat.api;

import java.util.Optional;

/**
 * Chooses which monster entity id to engage next.
 */
public interface ITargetSelectionStrategy {

    Optional<Integer> selectTarget(ICombatSnapshot snapshot, ICombatPolicy policy);
}
