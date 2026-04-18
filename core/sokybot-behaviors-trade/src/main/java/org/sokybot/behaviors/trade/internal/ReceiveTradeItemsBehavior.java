package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.MuleStallSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class ReceiveTradeItemsBehavior implements IBehavior<MuleStallSettings> {

    @Override
    public String id() {
        return "receive-trade-items";
    }

    @Override
    public int order() {
        return 30;
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
        context.log("INFO", "receive-trade-items placeholder for {}", context.getMachineId());
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 500L;
    }
}
