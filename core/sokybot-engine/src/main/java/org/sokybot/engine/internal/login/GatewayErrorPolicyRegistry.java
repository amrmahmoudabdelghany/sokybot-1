package org.sokybot.engine.internal.login;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.engine.api.login.GatewayFailureClassification;
import org.sokybot.engine.api.login.IGatewayErrorPolicy;
import org.sokybot.engine.api.login.IGatewayErrorPolicyRegistry;

@Component(service = IGatewayErrorPolicyRegistry.class, immediate = true)
public class GatewayErrorPolicyRegistry implements IGatewayErrorPolicyRegistry {

    private final List<IGatewayErrorPolicy> policies = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindPolicy(IGatewayErrorPolicy policy) {
        synchronized (policies) {
            policies.add(policy);
            policies.sort(Comparator.comparingInt(IGatewayErrorPolicy::getPriority).reversed());
        }
    }

    protected void unbindPolicy(IGatewayErrorPolicy policy) {
        policies.remove(policy);
    }

    @Override
    public Optional<GatewayFailureClassification> classify(Integer gatewayCode, String failureReason) {
        if (gatewayCode == null) {
            return Optional.empty();
        }
        for (IGatewayErrorPolicy policy : policies) {
            Optional<GatewayFailureClassification> result =
                    policy.classify(gatewayCode, failureReason);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
