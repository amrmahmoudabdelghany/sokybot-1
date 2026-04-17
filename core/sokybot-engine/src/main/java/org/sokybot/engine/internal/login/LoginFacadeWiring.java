package org.sokybot.engine.internal.login;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.engine.api.login.GatewayErrorClassifier;
import org.sokybot.engine.api.login.IGatewayErrorPolicyRegistry;
import org.sokybot.engine.api.login.ILoginPhaseResolverRegistry;
import org.sokybot.engine.api.login.LoginPhaseAliases;

/**
 * Connects login whiteboard registries to static script facades ({@link LoginPhaseAliases},
 * {@link GatewayErrorClassifier}).
 */
@Component(immediate = true)
public class LoginFacadeWiring {

    @Reference
    private ILoginPhaseResolverRegistry phaseRegistry;

    @Reference
    private IGatewayErrorPolicyRegistry policyRegistry;

    @Activate
    protected void activate() {
        LoginPhaseAliases.RESOLVER_REGISTRY_REF.set(phaseRegistry);
        GatewayErrorClassifier.POLICY_REGISTRY_REF.set(policyRegistry);
    }

    @Deactivate
    protected void deactivate() {
        LoginPhaseAliases.RESOLVER_REGISTRY_REF.set(null);
        GatewayErrorClassifier.POLICY_REGISTRY_REF.set(null);
    }
}
