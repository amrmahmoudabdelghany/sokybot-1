package org.sokybot.town.api;

import java.util.Optional;

/**
 * Chooses the next death-recovery macro step for the town / death orthogonal loop.
 */
public interface IDeathRecoveryStrategy {

    Optional<DeathRecoveryAction> nextStep(ITownSnapshot snapshot, ITownPolicy policy);
}
