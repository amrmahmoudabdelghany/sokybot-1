package org.sokybot.engine.internal.login;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.login.GatewayFailureClassification;
import org.sokybot.engine.api.login.IGatewayErrorPolicy;

class GatewayErrorPolicyRegistryTest {

    @Test
    void higherPriorityWins() {
        GatewayErrorPolicyRegistry reg = new GatewayErrorPolicyRegistry();
        reg.bindPolicy(new IGatewayErrorPolicy() {
            @Override
            public int getPriority() {
                return 25;
            }

            @Override
            public Optional<GatewayFailureClassification> classify(int gatewayCode, String failureReason) {
                return Optional.of(GatewayFailureClassification.UNKNOWN_RETRY);
            }
        });
        reg.bindPolicy(new IGatewayErrorPolicy() {
            @Override
            public int getPriority() {
                return 150;
            }

            @Override
            public Optional<GatewayFailureClassification> classify(int gatewayCode, String failureReason) {
                if (gatewayCode == 7) {
                    return Optional.of(GatewayFailureClassification.FATAL);
                }
                return Optional.empty();
            }
        });
        assertEquals(Optional.of(GatewayFailureClassification.FATAL), reg.classify(7, ""));
    }
}
