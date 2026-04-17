package org.sokybot.engine.api.login;

import java.util.Optional;

/**
 * Maps gateway login failure bytes (0xA102) to a coarse failure bucket. Policies must be
 * stateless. Built-ins should use {@code getPriority() &lt;= 25}.
 */
public interface IGatewayErrorPolicy {

    default int getPriority() {
        return 100;
    }

    Optional<GatewayFailureClassification> classify(int gatewayCode, String failureReason);
}
