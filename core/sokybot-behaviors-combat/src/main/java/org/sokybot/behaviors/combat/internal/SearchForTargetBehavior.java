package org.sokybot.behaviors.combat.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.settings.CombatSettings;
import org.sokybot.combat.api.CombatCycleKeys;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.ITargetSelectionStrategy;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class SearchForTargetBehavior implements IBehavior<CombatSettings> {

    @Reference
    private ICombatModel combatModel;

    @Reference
    private ITargetSelectionStrategy targetSelectionStrategy;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_SEARCH;
    }

    @Override
    public int order() {
        return 30;
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
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null) {
            return false;
        }
        if (snap.get().isSkillCastInFlight()) {
            return false;
        }
        return targetSelectionStrategy.selectTarget(snap.get(), settings.toPolicy()).isPresent();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        Optional<ICombatSnapshot> snap = combatModel.snapshot(context.getMachineId());
        if (!snap.isPresent() || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        Optional<Integer> tid = targetSelectionStrategy.selectTarget(snap.get(), settings.toPolicy());
        if (!tid.isPresent()) {
            return BehaviorStatus.SKIPPED;
        }
        CombatPackets.sendSelectEntity(context, tid.get().intValue());
        context.getPersistentData().put(CombatCycleKeys.KEY_COMBAT_PHASE, CombatCycleKeys.PHASE_HUNTING);
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 200L;
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }
}
