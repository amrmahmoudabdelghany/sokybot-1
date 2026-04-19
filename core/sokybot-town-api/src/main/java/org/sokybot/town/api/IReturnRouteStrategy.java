package org.sokybot.town.api;

import java.util.Optional;

/**
 * Chooses how to exit town and resume hunting once logistics are satisfied.
 */
public interface IReturnRouteStrategy {

    Optional<ReturnAction> nextStep(ITownSnapshot snapshot, ITownPolicy policy);
}
