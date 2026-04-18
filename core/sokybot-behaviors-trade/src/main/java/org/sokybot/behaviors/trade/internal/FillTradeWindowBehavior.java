package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.projections.api.ITradeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder: drive in-game exchange window fill using dispatcher (packet integration in a later phase).
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class FillTradeWindowBehavior implements IBehavior<FarmerTradeSettings> {

    private static final Logger log = LoggerFactory.getLogger(FillTradeWindowBehavior.class);

    @Override
    public String id() {
        return "fill-trade-window";
    }

    @Override
    public int order() {
        return 40;
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
        try {
            ITradeModel model = context.getService(ITradeModel.class);
            model.getSnapshot(context.getMachineId());
            context.log("DEBUG", "fill-trade-window snapshot check for {}", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.debug("fill-trade-window skipped: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 300L;
    }
}
