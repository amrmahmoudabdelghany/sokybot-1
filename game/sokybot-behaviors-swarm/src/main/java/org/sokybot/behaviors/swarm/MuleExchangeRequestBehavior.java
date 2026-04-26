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
import org.sokybot.swarm.api.LogisticsAbortedEvent;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.trade.events.TradeWindowOpened;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class MuleExchangeRequestBehavior implements IBehavior<LogisticsSettings> {

    private static final Logger log = LoggerFactory.getLogger(MuleExchangeRequestBehavior.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeCoordinator tradeCoordinator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IReactiveEventBus reactiveEventBus;

    @Reference
    private volatile SwarmJobMonitor jobMonitor;

    @Override
    public String id() {
        return "swarm-mule-exchange-request";
    }

    @Override
    public int order() {
        return 10;
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
        if (cfg == null || swarmBus == null || tradeCoordinator == null || reactiveEventBus == null) {
            return false;
        }
        SwarmRole role = cfg.getRole();
        if (role != SwarmRole.MULE && role != SwarmRole.BOTH) {
            return false;
        }
        if (!Boolean.TRUE.equals(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_ARRIVED))) {
            return false;
        }
        return tradeCoordinator.getSwarmSession(ctx.getMachineId()).isEmpty();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        Object rawReq = ctx.getPersistentData().get(LogisticsCycleKeys.KEY_ACTIVE_SWARM_REQUEST_ID);
        if (!(rawReq instanceof String)) {
            return BehaviorStatus.SKIPPED;
        }
        String reqId = (String) rawReq;
        Object fs = ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_FARMER_SPAWN_ID);
        long farmerSpawn = fs instanceof Number ? ((Number) fs).longValue() : 0L;
        Object fm = ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_FARMER_MACHINE_ID);
        String farmerMachine = fm instanceof String ? (String) fm : "";

        int expectFarmer = (int) farmerSpawn;
        TradeWindowOpened opened = TradeEventAwait.awaitNext(
                reactiveEventBus,
                TradeWindowOpened.class,
                e -> ctx.getMachineId().equals(e.getFullName()) && e.getOtherPlayerUniqueId() == expectFarmer,
                Duration.ofMillis(600));

        if (opened != null) {
            if (tradeCoordinator.openSwarmSession(ctx.getMachineId(), farmerMachine, reqId, SwarmRole.MULE)) {
                ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_ARRIVED);
                ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_EXCHANGE_ATTEMPTS);
                ctx.log("INFO", "Mule swarm session opened for {}", ctx.getMachineId());
            }
            return BehaviorStatus.EXECUTED;
        }

        int attempts = intFrom(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_EXCHANGE_ATTEMPTS));
        if (attempts >= 2) {
            swarmBus.publish(new LogisticsAbortedEvent(
                    farmerMachine, System.currentTimeMillis(), reqId, LogisticsAbortedEvent.Reason.EXCHANGE_CANCELLED));
            swarmBus.releaseClaim(reqId);
            if (tradeCoordinator != null) {
                tradeCoordinator.closeSwarmSession(ctx.getMachineId(), org.sokybot.trade.coordination.api.SwarmSessionResult.CANCELLED);
            }
            clearSwarm(ctx);
            jobMonitor.clearJob(ctx.getMachineId());
            log.warn("Mule exchange request failed after retries for {}", ctx.getMachineId());
            return BehaviorStatus.EXECUTED;
        }

        TradePackets.sendExchangeRequest(ctx, farmerSpawn);
        ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_EXCHANGE_ATTEMPTS, attempts + 1);
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

    private static int intFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }

    @Override
    public long postDelayMs() {
        return 500L;
    }
}
