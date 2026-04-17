package org.sokybot.login.policies.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.login.GatewayFailureClassification;

class BuiltinGatewayErrorPolicyTest {

    private final BuiltinGatewayErrorPolicy policy = new BuiltinGatewayErrorPolicy();

    @Test
    void priorityIsBuiltinFallback() {
        assertThat(policy.getPriority()).isEqualTo(25);
    }

    @Test
    void mapsKnownCodes() {
        assertThat(policy.classify(1, "")).contains(GatewayFailureClassification.CREDENTIAL);
        assertThat(policy.classify(0x0D, "")).contains(GatewayFailureClassification.CREDENTIAL);
        assertThat(policy.classify(4, "")).contains(GatewayFailureClassification.GHOST_COOLDOWN);
        assertThat(policy.classify(6, "")).contains(GatewayFailureClassification.NETWORK);
    }

    @Test
    void unknownCodeEmpty() {
        assertThat(policy.classify(99, "")).isEqualTo(Optional.empty());
    }
}
