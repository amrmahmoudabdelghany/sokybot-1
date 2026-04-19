package org.sokybot.behaviors.party.internal;

import java.util.Optional;

import org.sokybot.behaviors.party.internal.settings.PartySettings;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.PartyPolicy;
import org.sokybot.settings.api.ISettingsProvider;
import org.sokybot.settings.api.ISettingsRegistry;

/**
 * Resolves machine party policy from the {@code party} settings scope.
 */
public final class PartyPolicyResolver {

    private PartyPolicyResolver() {
    }

    public static IPartyPolicy resolve(IWorkflowContext ctx) {
        if (ctx == null) {
            return PartyPolicy.builder().build();
        }
        Optional<ISettingsRegistry> reg = ctx.getServiceOptional(ISettingsRegistry.class);
        if (!reg.isPresent()) {
            return PartyPolicy.builder().build();
        }
        ISettingsProvider<PartySettings> prov = reg.get().getProvider(
                ctx.getGroupName(), ctx.getMachineName(), "party", PartySettings.class);
        if (prov == null) {
            return PartyPolicy.builder().build();
        }
        PartySettings s = prov.get();
        if (s == null) {
            return PartyPolicy.builder().build();
        }
        return s.toPolicy();
    }
}
