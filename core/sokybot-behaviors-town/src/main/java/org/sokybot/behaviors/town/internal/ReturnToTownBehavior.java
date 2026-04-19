package org.sokybot.behaviors.town.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Coarse “get back to town / vendor bubble” step when inventory pressure is detected while alive.
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class ReturnToTownBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Override
    public String id() {
        return TownCycleKeys.BEHAVIOR_RETURN_TO_TOWN;
    }

    @Override
    public int order() {
        return 10;
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
        return snap.get().getInventory().getFreeSlots() <= settings.getMinFreeInventorySlots();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        context.log("INFO", "Town return-to-town (routing stub)");
        return BehaviorStatus.EXECUTED;
    }
}
