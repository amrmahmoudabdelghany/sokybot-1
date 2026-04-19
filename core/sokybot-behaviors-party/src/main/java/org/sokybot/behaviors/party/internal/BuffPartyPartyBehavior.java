package org.sokybot.behaviors.party.internal;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.party.internal.settings.PartySettings;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.party.api.IPartyModel;
import org.sokybot.party.api.IPartyPolicy;
import org.sokybot.party.api.IPartySnapshot;
import org.sokybot.party.api.PartyCycleKeys;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class BuffPartyPartyBehavior implements IBehavior<PartySettings> {

    @Reference
    private ICombatModel combatModel;

    @Reference
    private IPartyModel partyModel;

    @Override
    public String id() {
        return PartyCycleKeys.BEHAVIOR_BUFF_PARTY;
    }

    @Override
    public int order() {
        return 4;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return PartyCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<PartySettings> settingsType() {
        return PartySettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, PartySettings settings) {
        if (settings == null) {
            return false;
        }
        IPartyPolicy policy = settings.toPolicy();
        Optional<ICombatSnapshot> c = combatModel.snapshot(context.getMachineId());
        Optional<IPartySnapshot> p = partyModel.snapshot(context.getMachineId());
        if (!c.isPresent() || !p.isPresent()) {
            return false;
        }
        Map<String, Object> pd = context.getPersistentData();
        return PartyBuffShareLogic.shouldBuff(c.get(), p.get(), policy, pd);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, PartySettings settings) {
        if (settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        IPartyPolicy policy = settings.toPolicy();
        Optional<ICombatSnapshot> cOpt = combatModel.snapshot(context.getMachineId());
        Optional<IPartySnapshot> pOpt = partyModel.snapshot(context.getMachineId());
        if (!cOpt.isPresent() || !pOpt.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        Map<String, Object> pd = context.getPersistentData();
        return PartyBuffShareLogic.executeBuff(context, cOpt.get(), pOpt.get(), policy, pd);
    }

    @Override
    public long postDelayMs() {
        return 200L;
    }
}
