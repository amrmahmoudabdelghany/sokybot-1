package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder for final in-game trade confirmation packets.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class ConfirmTradeBehavior implements IBehavior<FarmerTradeSettings> {

    private static final Logger log = LoggerFactory.getLogger(ConfirmTradeBehavior.class);

    @Override
    public String id() {
        return "confirm-trade";
    }

    @Override
    public int order() {
        return 50;
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
        return settings != null && context.getPersistentData().containsKey(TradeKeys.COORDINATION_SESSION_ID);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, FarmerTradeSettings settings) {
        context.log("INFO", "confirm-trade placeholder for {}", context.getMachineId());
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 300L;
    }
}
