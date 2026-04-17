package org.sokybot.engine.internal.login;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.login.ILoginPhaseResolver;
import org.sokybot.gamemodel.LoginState;

class LoginPhaseResolverRegistryTest {

    @Test
    void higherPriorityWins() {
        LoginPhaseResolverRegistry reg = new LoginPhaseResolverRegistry();
        reg.bindResolver(new ILoginPhaseResolver() {
            @Override
            public int getPriority() {
                return 25;
            }

            @Override
            public Optional<LoginState.Phase> resolve(String rawPhase) {
                return Optional.of(LoginState.Phase.FAILED);
            }
        });
        reg.bindResolver(new ILoginPhaseResolver() {
            @Override
            public int getPriority() {
                return 200;
            }

            @Override
            public Optional<LoginState.Phase> resolve(String rawPhase) {
                if ("X".equals(rawPhase)) {
                    return Optional.of(LoginState.Phase.IN_QUEUE);
                }
                return Optional.empty();
            }
        });
        assertEquals(Optional.of(LoginState.Phase.IN_QUEUE), reg.resolve("X"));
    }
}
