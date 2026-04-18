package org.sokybot.behaviors.trade.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.coordination.api.FarmerProfile;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.MuleHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class DiscoverMuleBehavior implements IBehavior<FarmerTradeSettings> {

    private static final Logger log = LoggerFactory.getLogger(DiscoverMuleBehavior.class);

    @Override
    public String id() {
        return "discover-mule";
    }

    @Override
    public int order() {
        return 10;
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
        return settings != null;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, FarmerTradeSettings settings) {
        try {
            ITradeCoordinator coordinator = context.getService(ITradeCoordinator.class);
            Optional<MuleHandle> mule = coordinator.findBestMule(new FarmerProfile(context.getMachineId()));
            if (!mule.isPresent()) {
                context.log("INFO", "No mule available yet for {}", context.getMachineId());
                return BehaviorStatus.SKIPPED;
            }
            String id = mule.get().getMachineId();
            context.getPersistentData().put(TradeKeys.MULE_MACHINE_ID, id);
            context.log("INFO", "Selected mule {} for farmer {}", id, context.getMachineId());
            return BehaviorStatus.EXECUTED;
        } catch (RuntimeException ex) {
            log.warn("DiscoverMule failed: {}", ex.getMessage());
            return BehaviorStatus.SKIPPED;
        }
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
