package org.sokybot.engine.api.login;

import java.util.Optional;

/**
 * Aggregates {@link IGatewayErrorPolicy} services in priority order (highest first).
 */
@FunctionalInterface
public interface IGatewayErrorPolicyRegistry {

    Optional<GatewayFailureClassification> classify(Integer gatewayCode, String failureReason);
}
