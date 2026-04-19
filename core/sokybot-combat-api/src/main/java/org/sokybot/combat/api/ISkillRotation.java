package org.sokybot.combat.api;

import java.util.Optional;

/**
 * Chooses the next attack or buff action given cooldowns and snapshot.
 */
public interface ISkillRotation {

    Optional<SkillAction> nextAction(ICombatSnapshot snapshot, ICombatPolicy policy);
}
