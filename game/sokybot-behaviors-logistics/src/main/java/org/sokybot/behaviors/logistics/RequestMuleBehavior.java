package org.sokybot.behaviors.logistics;

import java.util.Optional;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.internal.LogisticsFarmPendingRegistry;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.LogisticsRequestEvent;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;
@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class RequestMuleBehavior implements IBehavior<LogisticsSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeCoordinator tradeCoordinator;

    @Reference
    private volatile LogisticsFarmPendingRegistry pendingRegistry;

    @Override
    public String id() {
        return "logistics-request-mule";
    }

    @Override
    public int order() {
        return 12;
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
        if (!cfg.isMuleSummonEnabled()) {
            return false;
        }
        if (swarmBus == null || townModel == null) {
            return false;
        }
        if (ctx.getPersistentData().containsKey(LogisticsCycleKeys.KEY_PENDING_REQUEST_ID)) {
            return false;
        }
        if (tradeCoordinator != null && tradeCoordinator.getSwarmSession(ctx.getMachineId()).isPresent()) {
            return false;
        }
        Optional<ITownSnapshot> snap = townModel.snapshot(ctx.getMachineId());
        if (!snap.isPresent() || snap.get().isDead()) {
            return false;
        }
        int total = snap.get().getInventory().getTotalSlots();
        int free = snap.get().getInventory().getFreeSlots();
        if (total <= 0) {
            return false;
        }
        double fillPct = ((double) (total - free)) / total;
        return fillPct >= cfg.getMuleSummonThreshold();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        ITownSnapshot snap = townModel.snapshot(ctx.getMachineId()).orElse(null);
        if (snap == null) {
            return BehaviorStatus.SKIPPED;
        }
        int uid = ctx.getGameModel().getTrainer().getUniqueId();
        String reqId = UUID.randomUUID().toString();
        long expires = System.currentTimeMillis() + cfg.getRequestTtlMs();

        LogisticsRequestEvent req = new LogisticsRequestEvent(
                ctx.getMachineId(),
                System.currentTimeMillis(),
                reqId,
                Math.round(snap.getSelfX()),
                Math.round(snap.getSelfZ()),
                Math.round(snap.getSelfY()),
                snap.getRegionId(),
                cfg.getDefaultPriority(),
                snap.getInventory().getFreeSlots(),
                uid,
                expires);

        swarmBus.publish(req);
        ctx.getPersistentData().put(LogisticsCycleKeys.KEY_PENDING_REQUEST_ID, reqId);
        ctx.getPersistentData().put(LogisticsCycleKeys.KEY_PENDING_REQUEST_AT_MS, System.currentTimeMillis());
        pendingRegistry.rememberPending(ctx.getMachineId(), reqId);
        ctx.log("INFO", "Published LogisticsRequestEvent {} for {}", reqId, ctx.getMachineId());
        return BehaviorStatus.EXECUTED;
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
        return 400L;
    }
}
