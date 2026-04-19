package org.sokybot.behaviors.combat.internal;

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
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.IStuckDetector;
import org.sokybot.navigation.api.NavigationException;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class UnstuckBehavior implements IBehavior<CombatSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IStuckDetector stuckDetector;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Override
    public String id() {
        return CombatCycleKeys.BEHAVIOR_UNSTUCK;
    }

    @Override
    public int order() {
        return 5;
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
        IStuckDetector det = stuckDetector;
        INavigator nav = navigator;
        if (det == null || nav == null || settings == null) {
            return false;
        }
        return det.isStuck(context.getMachineId());
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, CombatSettings settings) {
        IStuckDetector det = stuckDetector;
        INavigator nav = navigator;
        if (det == null || nav == null || settings == null) {
            return BehaviorStatus.SKIPPED;
        }
        try {
            nav.randomShortWalk(context, 24f);
        } catch (NavigationException e) {
            return BehaviorStatus.SKIPPED;
        }
        det.reset(context.getMachineId());
        context.getPersistentData().put(CombatCycleKeys.KEY_COMBAT_PHASE, CombatCycleKeys.PHASE_UNSTUCK);
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 350L;
    }

    @Override
    public boolean canInterrupt() {
        return false;
    }
}
