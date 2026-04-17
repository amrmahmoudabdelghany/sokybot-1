package org.sokybot.engine.api.login;

import java.util.Optional;

import org.sokybot.gamemodel.LoginState;

/**
 * Pluggable mapping from string engine phase names to {@link LoginState.Phase}. Built-in
 * resolvers should use {@code getPriority() &lt;= 25} so custom resolvers can override.
 */
public interface ILoginPhaseResolver {

    default int getPriority() {
        return 100;
    }

    /**
     * @param rawPhase non-null phase label from scripts or engine events
     */
    Optional<LoginState.Phase> resolve(String rawPhase);
}
