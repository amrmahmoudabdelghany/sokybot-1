package org.sokybot.behaviors.logistics;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.internal.LogisticsFarmPendingRegistry;
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

/**
 * Farmer waits for claim + exchange window; opens swarm session on {@link TradeWindowOpened}.
 */
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class AwaitMuleBehavior implements IBehavior<LogisticsSettings> {

    private static final Logger log = LoggerFactory.getLogger(AwaitMuleBehavior.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeCoordinator tradeCoordinator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IReactiveEventBus reactiveEventBus;

    @Reference
    private volatile LogisticsFarmPendingRegistry pendingRegistry;

    @Override
    public String id() {
        return "logistics-await-mule";
    }

    @Override
    public int order() {
        return 11;
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
        if (cfg == null) {
            return false;
        }
        SwarmRole role = cfg.getRole();
        if (role != SwarmRole.FARMER && role != SwarmRole.BOTH) {
            return false;
        }
        if (swarmBus == null || tradeCoordinator == null || reactiveEventBus == null) {
            return false;
        }
        if (!ctx.getPersistentData().containsKey(LogisticsCycleKeys.KEY_PENDING_REQUEST_ID)) {
            return false;
        }
        return tradeCoordinator.getSwarmSession(ctx.getMachineId()).isEmpty();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        Object rawReq = ctx.getPersistentData().get(LogisticsCycleKeys.KEY_PENDING_REQUEST_ID);
        if (!(rawReq instanceof String)) {
            return BehaviorStatus.SKIPPED;
        }
        String reqId = (String) rawReq;
        Object rawAt = ctx.getPersistentData().get(LogisticsCycleKeys.KEY_PENDING_REQUEST_AT_MS);
        long started = rawAt instanceof Number ? ((Number) rawAt).longValue() : 0L;
        long now = System.currentTimeMillis();
        if (started > 0L && now - started > cfg.getRequestTtlMs()) {
            swarmBus.publish(new LogisticsAbortedEvent(ctx.getMachineId(), now, reqId, LogisticsAbortedEvent.Reason.TIMEOUT));
            swarmBus.releaseClaim(reqId);
            clearPendingKeys(ctx);
            pendingRegistry.clearPending(ctx.getMachineId());
            return BehaviorStatus.EXECUTED;
        }

        Optional<String> claimMule = swarmBus.getClaimHolderMachineId(reqId);
        if (claimMule.isPresent()) {
            ctx.getPersistentData().put(LogisticsCycleKeys.KEY_CLAIMED_BY_MULE_ID, claimMule.get());
            OptionalLong muleUid = swarmBus.getClaimMuleTrainerUniqueId(reqId);
            muleUid.ifPresent(l -> ctx.getPersistentData().put(LogisticsCycleKeys.KEY_EXPECTED_MULE_TRAINER_UID, l));
        }

        OptionalLong expectedUid = OptionalLong.empty();
        Object uidObj = ctx.getPersistentData().get(LogisticsCycleKeys.KEY_EXPECTED_MULE_TRAINER_UID);
        if (uidObj instanceof Number) {
            expectedUid = OptionalLong.of(((Number) uidObj).longValue());
        }

        LogisticsAbortedEvent aborted = null;
        try {
            aborted = swarmBus
                    .observe(LogisticsAbortedEvent.class)
                    .filter(e -> reqId.equals(e.getRequestId()))
                    .next()
                    .timeout(Duration.ofMillis(120))
                    .blockOptional()
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.debug("Await swarm abort: {}", ex.toString());
        }
        if (aborted != null) {
            log.info("Logistics request {} aborted while awaiting mule: {}", reqId, aborted.getReason());
            clearPendingKeys(ctx);
            pendingRegistry.clearPending(ctx.getMachineId());
            return BehaviorStatus.EXECUTED;
        }

        if (expectedUid.isPresent()) {
            int expect = (int) expectedUid.getAsLong();
            TradeWindowOpened opened = null;
            try {
                opened = reactiveEventBus
                        .on(TradeWindowOpened.class)
                        .filter(e -> ctx.getMachineId().equals(e.getFullName()))
                        .filter(e -> e.getOtherPlayerUniqueId() == expect)
                        .next()
                        .timeout(Duration.ofMillis(800))
                        .blockOptional()
                        .orElse(null);
            } catch (RuntimeException ex) {
                log.debug("Await trade window: {}", ex.toString());
            }
            if (opened != null) {
                String muleId = (String) ctx.getPersistentData().get(LogisticsCycleKeys.KEY_CLAIMED_BY_MULE_ID);
                if (muleId != null
                        && tradeCoordinator.openSwarmSession(ctx.getMachineId(), muleId, reqId, SwarmRole.FARMER)) {
                    clearPendingKeys(ctx);
                    pendingRegistry.clearPending(ctx.getMachineId());
                    ctx.log("INFO", "Farmer swarm session opened after trade window for {}", ctx.getMachineId());
                    return BehaviorStatus.EXECUTED;
                }
            }
        }

        return BehaviorStatus.EXECUTED;
    }

    private static void clearPendingKeys(IWorkflowContext ctx) {
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_PENDING_REQUEST_ID);
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_PENDING_REQUEST_AT_MS);
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_CLAIMED_BY_MULE_ID);
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_EXPECTED_MULE_TRAINER_UID);
    }

    @Override
    public boolean canInterrupt() {
        return true;
    }

    @Override
    public int interruptionPriority() {
        return 80;
    }

    @Override
    public long postDelayMs() {
        return 350L;
    }
}
