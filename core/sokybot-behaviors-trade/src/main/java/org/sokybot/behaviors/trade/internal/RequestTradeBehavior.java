package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class RequestTradeBehavior implements IBehavior<FarmerTradeSettings> {

    private static final Logger log = LoggerFactory.getLogger(RequestTradeBehavior.class);

    @Override
    public String id() {
        return "request-trade";
    }

    @Override
    public int order() {
        return 30;
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
        try {
            Object raw = context.getPersistentData().get(TradeKeys.MULE_MACHINE_ID);
            if (!(raw instanceof String)) {
                return BehaviorStatus.SKIPPED;
            }
            String muleId = (String) raw;
            ITradeCoordinator coordinator = context.getService(ITradeCoordinator.class);
            String sessionId = coordinator.openTradeSession(context.getMachineId(), muleId);
            context.getPersistentData().put(TradeKeys.COORDINATION_SESSION_ID, sessionId);
            context.log("INFO", "Opened coordination session {} farmer {} -> mule {}", sessionId,
                    context.getMachineId(), muleId);
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.warn("request-trade failed: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
