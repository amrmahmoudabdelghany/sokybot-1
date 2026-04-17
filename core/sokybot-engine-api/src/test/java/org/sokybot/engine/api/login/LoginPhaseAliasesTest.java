package org.sokybot.engine.api.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sokybot.gamemodel.LoginState;

class LoginPhaseAliasesTest {

    @AfterEach
    void tearDown() {
        LoginPhaseAliases.RESOLVER_REGISTRY_REF.set(null);
    }

    @Test
    void resolvesEnumNameLikeValueOf() {
        assertEquals(Optional.of(LoginState.Phase.IN_QUEUE), LoginPhaseAliases.resolve("IN_QUEUE"));
    }

    @Test
    void resolvesGatewayLoginPauseAlias() {
        assertEquals(Optional.of(LoginState.Phase.AGENTS_RECEIVED),
                LoginPhaseAliases.resolve("GatewayLoginPause"));
    }

    @Test
    void registryOverridesBeforeDefaults() {
        LoginPhaseAliases.RESOLVER_REGISTRY_REF.set(phase -> {
            if ("CUSTOM_ALIAS".equals(phase)) {
                return Optional.of(LoginState.Phase.WAIT_FOR_CAPTCHA);
            }
            return Optional.empty();
        });
        assertEquals(Optional.of(LoginState.Phase.WAIT_FOR_CAPTCHA),
                LoginPhaseAliases.resolve("CUSTOM_ALIAS"));
    }

    @Test
    void blankReturnsEmpty() {
        assertTrue(LoginPhaseAliases.resolve(null).isEmpty());
        assertTrue(LoginPhaseAliases.resolve("").isEmpty());
    }
}
