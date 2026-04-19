package org.sokybot.behaviors.town.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.town.api.IIntentArbiter;
import org.sokybot.town.api.IRepairProvider;
import org.sokybot.town.api.IRestockProvider;
import org.sokybot.town.api.IStashProvider;
import org.sokybot.town.api.IntentKind;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class HandoffBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Reference
    private IIntentArbiter intentArbiter;

    @Reference
    private IRestockProvider restockProvider;

    @Reference
    private IRepairProvider repairProvider;

    @Reference
    private IStashProvider stashProvider;

    @Override
    public String id() {
        return TownCycleKeys.BEHAVIOR_HANDOFF;
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return TownCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<TownSettings> settingsType() {
        return TownSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, TownSettings settings) {
        if (settings == null || !settings.isTownLoopEnabled()) {
            return false;
        }
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || snap.get().isDead()) {
            return false;
        }
        var pol = settings.toPolicy();
        boolean pending = restockProvider.needs(snap.get().getInventory(), pol).isPresent()
                || repairProvider.needs(snap.get().getDurability(), pol).isPresent()
                || stashProvider.needs(snap.get().getInventory(), pol).isPresent();
        IntentKind cur = intentArbiter.decide(context.getMachineId());
        return cur == IntentKind.TOWN && !pending;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        intentArbiter.release(context.getMachineId(), IntentKind.TOWN);
        context.getPersistentData().put(TownCycleKeys.LAST_INTENT_KIND, IntentKind.COMBAT.name());
        context.log("INFO", "Town hand-off: suppressed TOWN intent briefly for combat scheduling");
        return BehaviorStatus.EXECUTED;
    }
}
