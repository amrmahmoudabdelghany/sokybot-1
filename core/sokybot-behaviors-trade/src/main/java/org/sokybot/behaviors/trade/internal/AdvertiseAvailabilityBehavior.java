package org.sokybot.behaviors.trade.internal;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.MuleStallSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.MuleIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class AdvertiseAvailabilityBehavior implements IBehavior<MuleStallSettings> {

    private static final Logger log = LoggerFactory.getLogger(AdvertiseAvailabilityBehavior.class);

    @Override
    public String id() {
        return "advertise-availability";
    }

    @Override
    public int order() {
        return 10;
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
            ITradeCoordinator coordinator = context.getService(ITradeCoordinator.class);
            MuleIntent intent = new MuleIntent(context.getMachineId(), settings.getFreeCargoSlots(),
                    settings.getStallLocationHint());
            coordinator.registerMule(intent);
            context.log("INFO", "Mule {} registered with coordinator", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.warn("advertise-availability failed: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
