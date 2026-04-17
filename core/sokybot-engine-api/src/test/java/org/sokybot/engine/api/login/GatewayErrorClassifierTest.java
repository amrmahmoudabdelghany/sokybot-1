package org.sokybot.engine.api.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GatewayErrorClassifierTest {

    @AfterEach
    void tearDown() {
        GatewayErrorClassifier.POLICY_REGISTRY_REF.set(null);
    }

    @Test
    void defaultsUsedWhenRegistryUnset() {
        assertEquals(Optional.of(GatewayFailureClassification.GHOST_COOLDOWN),
                GatewayErrorClassifier.classify(4, ""));
        assertEquals(Optional.of(GatewayFailureClassification.NETWORK),
                GatewayErrorClassifier.classify(6, ""));
        assertEquals(Optional.of(GatewayFailureClassification.CREDENTIAL),
                GatewayErrorClassifier.classify(1, ""));
    }

    @Test
    void registryWinsOverDefaults() {
        GatewayErrorClassifier.POLICY_REGISTRY_REF.set((code, reason) -> {
            if (Integer.valueOf(4).equals(code)) {
                return Optional.of(GatewayFailureClassification.FATAL);
            }
            return Optional.empty();
        });
        assertEquals(Optional.of(GatewayFailureClassification.FATAL),
                GatewayErrorClassifier.classify(4, ""));
    }

    @Test
    void registryReturningEmptyUsesDefaults() {
        GatewayErrorClassifier.POLICY_REGISTRY_REF.set((code, reason) -> Optional.empty());
        assertEquals(Optional.of(GatewayFailureClassification.CREDENTIAL),
                GatewayErrorClassifier.classify(2, ""));
    }

    @Test
    void nullCodeEmpty() {
        assertTrue(GatewayErrorClassifier.classify(null, "").isEmpty());
    }
}
