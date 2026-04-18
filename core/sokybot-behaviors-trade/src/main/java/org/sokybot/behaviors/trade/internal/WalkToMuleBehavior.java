package org.sokybot.behaviors.trade.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.game.navigation.IRouteFinder;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class WalkToMuleBehavior implements IBehavior<FarmerTradeSettings> {

    @Override
    public String id() {
        return "walk-to-mule";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return "trade-cycle".equals(cycleId);
    }

    @Override
    public Class<FarmerTradeSettings> settingsType() {
        return FarmerTradeSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, FarmerTradeSettings settings) {
        return settings != null && context.getPersistentData().containsKey(TradeKeys.MULE_MACHINE_ID);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, FarmerTradeSettings settings) {
        Optional<IRouteFinder> finder = context.getServiceOptional(IRouteFinder.class);
        if (!finder.isPresent()) {
            context.log("DEBUG", "IRouteFinder unavailable; skipping walk-to-mule stub for {}", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        }
        context.log("INFO", "Walk-to-mule placeholder (pathing integration pending) for {}", context.getMachineId());
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 500L;
    }
}
