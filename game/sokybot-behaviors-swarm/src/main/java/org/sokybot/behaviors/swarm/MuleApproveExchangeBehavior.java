package org.sokybot.behaviors.swarm;

import java.time.Duration;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.LogisticsCycleKeys;
import org.sokybot.behaviors.logistics.LogisticsSettings;
import org.sokybot.behaviors.trade.packets.TradeEventAwait;
import org.sokybot.behaviors.trade.packets.TradePackets;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.trade.coordination.api.SwarmSessionPhase;
import org.sokybot.trade.events.TradeItemAdded;
import org.sokybot.trade.projections.api.ITradeModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class MuleApproveExchangeBehavior implements IBehavior<LogisticsSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeCoordinator tradeCoordinator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IReactiveEventBus reactiveEventBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeModel tradeModel;

    @Override
    public String id() {
        return "swarm-mule-approve-exchange";
    }

    @Override
    public int order() {
        return 13;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return LogisticsCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<LogisticsSettings> settingsType() {
        return LogisticsSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext ctx, LogisticsSettings cfg) {
        if (cfg == null || tradeCoordinator == null || reactiveEventBus == null || tradeModel == null) {
            return false;
        }
        SwarmRole role = cfg.getRole();
        if (role != SwarmRole.MULE && role != SwarmRole.BOTH) {
            return false;
        }
        if (Boolean.TRUE.equals(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_MULE_APPROVE_SENT))) {
            return false;
        }
        return tradeCoordinator
                .getSwarmSession(ctx.getMachineId())
                .map(s -> s.getPhase() == SwarmSessionPhase.FILLING)
                .orElse(false);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        if (!tradeModel.getSnapshot(ctx.getMachineId()).map(s -> s.isExchangeActive() && s.getExchangeId() >= 0).orElse(false)) {
            return BehaviorStatus.SKIPPED;
        }

        TradeItemAdded partnerItem = TradeEventAwait.awaitNext(
                reactiveEventBus,
                TradeItemAdded.class,
                e -> ctx.getMachineId().equals(e.getFullName()) && !e.isSelfOffer() && e.getItemRefId() > 0,
                Duration.ofMillis(1500));

        if (partnerItem == null) {
            return BehaviorStatus.EXECUTED;
        }

        TradePackets.sendExchangeApprove(ctx);
        tradeCoordinator.updateSwarmSessionPhase(ctx.getMachineId(), SwarmSessionPhase.ACCEPTED);
        ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_MULE_APPROVE_SENT, Boolean.TRUE);
        return BehaviorStatus.EXECUTED;
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
