package org.sokybot.behaviors.town.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.town.internal.settings.TownSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.town.api.DeathRecoveryAction;
import org.sokybot.town.api.IDeathRecoveryStrategy;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.TownCycleKeys;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class DeathRecoveryBehavior implements IBehavior<TownSettings> {

    @Reference
    private ITownModel townModel;

    @Reference
    private IDeathRecoveryStrategy deathRecoveryStrategy;

    @Override
    public String id() {
        return TownCycleKeys.BEHAVIOR_DEATH_RECOVERY;
    }

    @Override
    public int order() {
        return 5;
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
        if (!snap.isPresent()) {
            return false;
        }
        return snap.get().isDead()
                && deathRecoveryStrategy.nextStep(snap.get(), settings.toPolicy()).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, TownSettings settings) {
        Optional<ITownSnapshot> snap = townModel.snapshot(context.getMachineId());
        if (!snap.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<DeathRecoveryAction> step = deathRecoveryStrategy.nextStep(snap.get(), settings.toPolicy());
        if (!step.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        context.getPersistentData().put(TownCycleKeys.ACTIVE_DEATH_ACTION, step.get());
        context.log("INFO", "Town death-recovery: {}", step.get().getKind());
        return BehaviorStatus.EXECUTED;
    }
}
