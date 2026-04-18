package org.sokybot.behaviors.trade.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.MuleStallSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.game.navigation.IRouteFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class WalkToStallAreaBehavior implements IBehavior<MuleStallSettings> {

    private static final Logger log = LoggerFactory.getLogger(WalkToStallAreaBehavior.class);

    @Override
    public String id() {
        return "walk-to-stall-area";
    }

    @Override
    public int order() {
        return 40;
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
        Optional<IRouteFinder> finder = context.getServiceOptional(IRouteFinder.class);
        if (!finder.isPresent()) {
            context.log("DEBUG", "No IRouteFinder; walk-to-stall-area noop for {}", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        }
        context.log("INFO", "walk-to-stall-area placeholder for {}", context.getMachineId());
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 600L;
    }
}
