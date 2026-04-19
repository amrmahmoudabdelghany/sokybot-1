package org.sokybot.behaviors.party.internal;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;
import org.sokybot.party.coordination.api.IPartyCoordinator;

/**
 * Auto-accept (combat-cycle) using party policy from the {@code party} settings scope.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class AutoAcceptInviteCombatBehavior implements IBehavior<CombatSettings> {

    private static final String BEHAVIOR_ID = PartyCycleKeys.BEHAVIOR_AUTO_ACCEPT + "Combat";

    @Reference
    private IPartyModel partyModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPartyCoordinator partyCoordinator;

    @Override
    public String id() {
        return BEHAVIOR_ID;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return CombatCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<CombatSettings> settingsType() {
        return CombatSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, CombatSettings settings) {
        IPartyPolicy policy = PartyPolicyResolver.resolve(context);
        Optional<IPartySnapshot> snap = partyModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return false;
        }
        Map<String, Object> pd = context.getPersistentData();
        return PartyAutoAcceptLogic.shouldRun(context, snap.get(), pd, policy);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        IPartyPolicy policy = PartyPolicyResolver.resolve(context);
        Optional<IPartySnapshot> snap = partyModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        return PartyAutoAcceptLogic.execute(context, policy, partyCoordinator, snap.get());
    }
}
