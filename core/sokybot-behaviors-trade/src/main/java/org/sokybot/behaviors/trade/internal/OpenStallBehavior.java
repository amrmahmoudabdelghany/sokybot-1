package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.MuleStallSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class OpenStallBehavior implements IBehavior<MuleStallSettings> {

    private static final Logger log = LoggerFactory.getLogger(OpenStallBehavior.class);

    @Override
    public String id() {
        return "open-stall";
    }

    @Override
    public int order() {
        return 50;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return "stall-cycle".equals(cycleId);
    }

    @Override
    public Class<MuleStallSettings> settingsType() {
        return MuleStallSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext context, MuleStallSettings settings) {
        return settings != null;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, MuleStallSettings settings) {
        try {
            int stallId = context.getPersistentData().containsKey(TradeKeys.STALL_ENTITY_ID)
                    ? ((Number) context.getPersistentData().get(TradeKeys.STALL_ENTITY_ID)).intValue()
                    : context.getMachineId().hashCode();
            ITradeCoordinator coordinator = context.getService(ITradeCoordinator.class);
            coordinator.publishStallReady(context.getMachineId(), Integer.toString(stallId));
            context.log("INFO", "Published stall-ready for {}", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.warn("open-stall failed: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
