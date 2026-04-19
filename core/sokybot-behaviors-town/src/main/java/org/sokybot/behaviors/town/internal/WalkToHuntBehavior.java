package org.sokybot.behaviors.town.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.town.api.IReturnRouteStrategy;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.ReturnActionKind;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class WalkToHuntBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Reference
    private IReturnRouteStrategy returnRouteStrategy;

    @Override
    public String id() {
        return TownCycleKeys.BEHAVIOR_WALK_TO_HUNT;
    }

    @Override
    public int order() {
        return 60;
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
        return returnRouteStrategy.nextStep(snap.get(), settings.toPolicy())
                .filter(a -> a.getKind() == ReturnActionKind.WALK_TO_GATE)
                .isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        returnRouteStrategy.nextStep(snap.get(), settings.toPolicy())
                .ifPresent(a -> context.getPersistentData().put(TownCycleKeys.ACTIVE_RETURN_ACTION, a));
        context.log("INFO", "Town walk-to-hunt / leave town");
        return BehaviorStatus.EXECUTED;
    }
}
