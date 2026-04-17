package org.sokybot.login.policies.builtin;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.engine.api.login.GatewayFailureClassification;
import org.sokybot.engine.api.login.GatewayLoginErrorDefaults;
import org.sokybot.engine.api.login.IGatewayErrorPolicy;

/**
 * Default gateway 0xA102 failure-byte mapping (delegates to {@link GatewayLoginErrorDefaults}).
 */
@Component(service = IGatewayErrorPolicy.class)
public class BuiltinGatewayErrorPolicy implements IGatewayErrorPolicy {

    public BuiltinGatewayErrorPolicy() {
    }

    @Override
    public int getPriority() {
        return 25;
    }

    @Override
    public Optional<GatewayFailureClassification> classify(final int gatewayCode,
            @SuppressWarnings("unused") final String failureReason) {
        return GatewayLoginErrorDefaults.classify(gatewayCode);
    }
}
