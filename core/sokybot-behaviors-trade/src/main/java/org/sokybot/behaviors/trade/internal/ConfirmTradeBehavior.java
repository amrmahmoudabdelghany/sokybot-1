package org.sokybot.behaviors.trade.internal;

import java.time.Duration;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.trade.internal.settings.FarmerTradeSettings;
import org.sokybot.behaviors.trade.packets.TradeEventAwait;
import org.sokybot.behaviors.trade.packets.TradePackets;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.LogisticsCompletedEvent;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.trade.coordination.api.SwarmSessionPhase;
import org.sokybot.trade.coordination.api.SwarmSessionResult;
import org.sokybot.trade.events.TradeExchangeApproved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Farmer finalizes exchange; swarm path sends {@code EXCHANGE_FINALIZE} and completes the swarm session.
 */
@Component(service = IBehavior.class, immediate = true, scope = ServiceScope.PROTOTYPE)
public final class ConfirmTradeBehavior implements IBehavior<FarmerTradeSettings> {

    private static final Logger log = LoggerFactory.getLogger(ConfirmTradeBehavior.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IReactiveEventBus reactiveEventBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

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
        if (settings == null) {
            return false;
        }
        Optional<ITradeCoordinator> coord = context.getServiceOptional(ITradeCoordinator.class);
        if (!coord.isPresent()) {
            return false;
        }
        Optional<org.sokybot.trade.coordination.api.SwarmSessionState> sm =
                coord.get().getSwarmSession(context.getMachineId());
        boolean swarmFinalize = sm.isPresent()
                && sm.get().getLocalRole() == SwarmRole.FARMER
                && sm.get().getPhase() == SwarmSessionPhase.ACCEPTED;
        boolean stall = context.getPersistentData().containsKey(TradeKeys.COORDINATION_SESSION_ID);
        return swarmFinalize || stall;
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext context, FarmerTradeSettings settings) {
        ITradeCoordinator coordinator = context.getService(ITradeCoordinator.class);
        Optional<org.sokybot.trade.coordination.api.SwarmSessionState> sm =
                coordinator.getSwarmSession(context.getMachineId());
        boolean swarmFarmer = sm.isPresent()
                && sm.get().getLocalRole() == SwarmRole.FARMER
                && sm.get().getPhase() == SwarmSessionPhase.ACCEPTED;

        if (swarmFarmer && reactiveEventBus != null) {
            TradePackets.sendExchangeFinalize(context);
            TradeExchangeApproved approved = TradeEventAwait.awaitNext(
                    reactiveEventBus,
                    TradeExchangeApproved.class,
                    e -> context.getMachineId().equals(e.getFullName()),
                    Duration.ofSeconds(12));
            if (approved != null && approved.isSuccess()) {
                String reqId = sm.get().getRequestId();
                coordinator.closeSwarmSession(context.getMachineId(), SwarmSessionResult.COMPLETED);
                if (swarmBus != null) {
                    swarmBus.publish(new LogisticsCompletedEvent(context.getMachineId(), System.currentTimeMillis(), reqId));
                }
                context.getPersistentData().remove(TradeKeys.SWARM_FILL_SLOT_CURSOR);
                context.getPersistentData().remove(TradeKeys.SWARM_OFFER_SLOT_CURSOR);
            } else {
                coordinator.closeSwarmSession(context.getMachineId(), SwarmSessionResult.ERROR);
            }
            context.log("INFO", "confirm-trade swarm finalize for {}", context.getMachineId());
            return BehaviorStatus.EXECUTED;
        }

        context.log("INFO", "confirm-trade placeholder for {}", context.getMachineId());
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 300L;
    }
}
