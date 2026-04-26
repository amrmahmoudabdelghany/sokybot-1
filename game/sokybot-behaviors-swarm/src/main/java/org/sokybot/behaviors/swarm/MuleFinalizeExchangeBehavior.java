package org.sokybot.behaviors.swarm;

import java.time.Duration;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.LogisticsCycleKeys;
import org.sokybot.behaviors.logistics.LogisticsSettings;
import org.sokybot.behaviors.swarm.internal.SwarmJobMonitor;
import org.sokybot.behaviors.trade.packets.TradeEventAwait;
import org.sokybot.behaviors.trade.packets.TradePackets;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.trade.coordination.api.SwarmSessionPhase;
import org.sokybot.trade.coordination.api.SwarmSessionResult;
import org.sokybot.trade.events.TradeExchangeApproved;
import org.sokybot.trade.events.TradePartnerConfirmed;
import org.sokybot.trade.projections.api.ITradeModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class MuleFinalizeExchangeBehavior implements IBehavior<LogisticsSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeCoordinator tradeCoordinator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IReactiveEventBus reactiveEventBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeModel tradeModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference
    private volatile SwarmJobMonitor jobMonitor;

    @Override
    public String id() {
        return "swarm-mule-finalize-exchange";
    }

    @Override
    public int order() {
        return 14;
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
        if (!Boolean.TRUE.equals(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_MULE_APPROVE_SENT))) {
            return false;
        }
        if (Boolean.TRUE.equals(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_MULE_FINALIZE_SENT))) {
            return false;
        }
        return tradeCoordinator
                .getSwarmSession(ctx.getMachineId())
                .map(s -> s.getPhase() == SwarmSessionPhase.ACCEPTED)
                .orElse(false);
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        TradeEventAwait.awaitNext(
                reactiveEventBus,
                TradePartnerConfirmed.class,
                e -> ctx.getMachineId().equals(e.getFullName()) && !e.isSelfConfirmed(),
                Duration.ofMillis(800));

        TradePackets.sendExchangeFinalize(ctx);
        ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_MULE_FINALIZE_SENT, Boolean.TRUE);

        TradeExchangeApproved approved = TradeEventAwait.awaitNext(
                reactiveEventBus,
                TradeExchangeApproved.class,
                e -> ctx.getMachineId().equals(e.getFullName()),
                Duration.ofSeconds(8));

        Object rawReq = ctx.getPersistentData().get(LogisticsCycleKeys.KEY_ACTIVE_SWARM_REQUEST_ID);
        String reqId = rawReq instanceof String ? (String) rawReq : "";

        if (approved != null && approved.isSuccess()) {
            tradeCoordinator.closeSwarmSession(ctx.getMachineId(), SwarmSessionResult.COMPLETED);
            if (swarmBus != null && !reqId.isEmpty()) {
                swarmBus.releaseClaim(reqId);
            }
        } else {
            tradeCoordinator.closeSwarmSession(ctx.getMachineId(), SwarmSessionResult.ERROR);
            if (swarmBus != null && !reqId.isEmpty()) {
                swarmBus.releaseClaim(reqId);
            }
        }

        clearSwarm(ctx);
        jobMonitor.clearJob(ctx.getMachineId());
        return BehaviorStatus.EXECUTED;
    }

    private static void clearSwarm(IWorkflowContext ctx) {
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_ACTIVE_SWARM_REQUEST_ID);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_TARGET_X);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_TARGET_Z);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_TARGET_Y);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_FARMER_SPAWN_ID);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_FARMER_MACHINE_ID);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_ARRIVED);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_JOB_STARTED_MS);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_EXCHANGE_ATTEMPTS);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_JOB_MONITOR_REGISTERED);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_MULE_APPROVE_SENT);
        ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_MULE_FINALIZE_SENT);
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }
}
