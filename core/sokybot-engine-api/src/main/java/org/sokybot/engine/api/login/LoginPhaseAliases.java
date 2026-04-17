package org.sokybot.engine.api.login;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.sokybot.gamemodel.LoginState;

/**
 * Script-facing facade: resolves engine phase strings to {@link LoginState.Phase}, including
 * legacy aliases when {@link LoginState.Phase#valueOf(String)} does not apply.
 */
public final class LoginPhaseAliases {

    private static final Map<String, LoginState.Phase> DEFAULT_ALIASES;

    /** Wired by the engine bundle when OSGi declarative services activate. */
    public static final AtomicReference<ILoginPhaseResolverRegistry> RESOLVER_REGISTRY_REF =
            new AtomicReference<>();

    static {
        Map<String, LoginState.Phase> m = new HashMap<>();
        m.put("GatewayLoginPause", LoginState.Phase.AGENTS_RECEIVED);
        DEFAULT_ALIASES = Collections.unmodifiableMap(m);
    }

    private LoginPhaseAliases() {
    }

    /**
     * Default alias table (immutable); built-in OSGi resolvers delegate here.
     */
    public static Map<String, LoginState.Phase> defaultAliases() {
        return DEFAULT_ALIASES;
    }

    /**
     * Resolve a phase label: enum name first, then registry (if wired), then default aliases.
     */
    public static Optional<LoginState.Phase> resolve(String rawPhase) {
        if (rawPhase == null || rawPhase.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LoginState.Phase.valueOf(rawPhase));
        } catch (IllegalArgumentException ignored) {
            // Fall through for legacy aliases.
        }
        ILoginPhaseResolverRegistry registry = RESOLVER_REGISTRY_REF.get();
        if (registry != null) {
            Optional<LoginState.Phase> fromRegistry = registry.resolve(rawPhase);
            if (fromRegistry.isPresent()) {
                return fromRegistry;
            }
        }
        return Optional.ofNullable(DEFAULT_ALIASES.get(rawPhase));
    }
}
