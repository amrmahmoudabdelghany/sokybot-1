package org.sokybot.combat.api;

import java.util.Optional;

/**
 * Chooses potions or escape actions based on HP/MP and status.
 */
public interface IRecoveryStrategy {

    Optional<RecoveryAction> nextRecovery(ICombatSnapshot snapshot, ICombatPolicy policy);
}
