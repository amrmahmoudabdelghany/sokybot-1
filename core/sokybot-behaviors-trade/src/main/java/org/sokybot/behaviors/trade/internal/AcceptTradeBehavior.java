package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.MuleStallSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.projections.api.ITradeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Placeholder for accepting incoming coordination / in-game trade invite.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class AcceptTradeBehavior implements IBehavior<MuleStallSettings> {

    private static final Logger log = LoggerFactory.getLogger(AcceptTradeBehavior.class);

    @Override
    public String id() {
        return "accept-trade";
    }

    @Override
    public int order() {
        return 20;
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
            context.getServiceOptional(ITradeModel.class).ifPresent(m -> m.getSnapshot(context.getMachineId()));
            context.log("DEBUG", "accept-trade placeholder for mule {}", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.debug("accept-trade: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
