package org.sokybot.login.policies.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sokybot.gamemodel.LoginState;

class BuiltinLoginPhaseResolverTest {

    private final BuiltinLoginPhaseResolver resolver = new BuiltinLoginPhaseResolver();

    @Test
    void priorityIsBuiltinFallback() {
        assertThat(resolver.getPriority()).isEqualTo(25);
    }

    @Test
    void resolvesGatewayLoginPauseAlias() {
        assertThat(resolver.resolve("GatewayLoginPause")).contains(LoginState.Phase.AGENTS_RECEIVED);
    }

    @Test
    void unknownAliasEmpty() {
        assertThat(resolver.resolve("NOT_AN_ALIAS")).isEqualTo(Optional.empty());
    }
}
