package org.sokybot.login.policies.builtin;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.login.ILoginPhaseResolver;
import org.sokybot.engine.api.login.LoginPhaseAliases;
import org.sokybot.gamemodel.LoginState;

/**
 * Default string-to-enum phase resolution (alias table in {@link LoginPhaseAliases}).
 */
@Component(service = ILoginPhaseResolver.class)
public class BuiltinLoginPhaseResolver implements ILoginPhaseResolver {

    public BuiltinLoginPhaseResolver() {
    }

    @Override
    public int getPriority() {
        return 25;
    }

    @Override
    public Optional<LoginState.Phase> resolve(final String rawPhase) {
        Optional<LoginState.Phase> resolved = Optional.empty();
        if (rawPhase != null) {
            Map<String, LoginState.Phase> aliases = LoginPhaseAliases.defaultAliases();
            resolved = Optional.ofNullable(aliases.get(rawPhase));
        }
        return resolved;
    }
}
