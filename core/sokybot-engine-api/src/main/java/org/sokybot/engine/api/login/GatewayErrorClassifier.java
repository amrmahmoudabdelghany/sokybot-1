package org.sokybot.engine.api.login;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Facade for gateway failure-byte classification via the OSGi {@link IGatewayErrorPolicy}
 * whiteboard, with {@link GatewayLoginErrorDefaults} when no policy matches or the registry is
 * not wired.
 */
public final class GatewayErrorClassifier {

    /** Wired by the engine bundle when OSGi declarative services activate. */
    public static final AtomicReference<IGatewayErrorPolicyRegistry> POLICY_REGISTRY_REF =
            new AtomicReference<>();

    private GatewayErrorClassifier() {
    }

    /**
     * Classifies a gateway failure byte; {@code failureReason} is forwarded to policies for
     * forward-compatible keyword handling.
     */
    public static Optional<GatewayFailureClassification> classify(Integer gatewayCode,
            String failureReason) {
        if (gatewayCode == null) {
            return Optional.empty();
        }
        IGatewayErrorPolicyRegistry registry = POLICY_REGISTRY_REF.get();
        if (registry != null) {
            Optional<GatewayFailureClassification> fromRegistry =
                    registry.classify(gatewayCode, failureReason);
            if (fromRegistry.isPresent()) {
                return fromRegistry;
            }
        }
        return GatewayLoginErrorDefaults.classify(gatewayCode);
    }
}
