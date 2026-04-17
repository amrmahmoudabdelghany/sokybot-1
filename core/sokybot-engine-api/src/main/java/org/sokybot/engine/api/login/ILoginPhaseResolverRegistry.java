package org.sokybot.engine.api.login;

import java.util.Optional;

import org.sokybot.gamemodel.LoginState;

/**
 * Aggregates {@link ILoginPhaseResolver} services in priority order (highest first).
 */
@FunctionalInterface
public interface ILoginPhaseResolverRegistry {

    Optional<LoginState.Phase> resolve(String rawPhase);
}
