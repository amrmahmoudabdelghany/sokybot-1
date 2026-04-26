package org.sokybot.behaviors.swarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.LogisticsCycleKeys;
import org.sokybot.behaviors.logistics.LogisticsSettings;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.LogisticsRequestEvent;
import org.sokybot.topology.api.ITeleportRouter;
import org.sokybot.topology.api.TeleportRoute;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.projections.api.ITownModel;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class MuleClaimBehavior implements IBehavior<LogisticsSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITownModel townModel;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITeleportRouter router;

    @Override
    public String id() {
        return "swarm-mule-claim";
    }

    @Override
    public int order() {
        return 8;
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
        if (cfg == null || swarmBus == null) {
            return false;
        }
        SwarmRole role = cfg.getRole();
        if (role != SwarmRole.MULE && role != SwarmRole.BOTH) {
            return false;
        }
        if (ctx.getPersistentData().get(LogisticsCycleKeys.KEY_ACTIVE_SWARM_REQUEST_ID) != null) {
            return false;
        }
        if (townModel != null) {
            ITownSnapshot snap = townModel.snapshot(ctx.getMachineId()).orElse(null);
            if (snap == null || snap.getInventory().getFreeSlots() < cfg.getMinMuleFreeSlots()) {
                return false;
            }
        }
        return !pickEligibleRequests(ctx, cfg).isEmpty();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        int muleUid = ctx.getGameModel().getTrainer().getUniqueId();
        for (LogisticsRequestEvent req : pickEligibleRequests(ctx, cfg)) {
            if (swarmBus.tryClaim(req.getRequestId(), ctx.getMachineId(), muleUid)) {
                ctx.getPersistentData().put(LogisticsCycleKeys.KEY_ACTIVE_SWARM_REQUEST_ID, req.getRequestId());
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_TARGET_X, req.getWorldX());
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_TARGET_Z, req.getWorldZ());
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_TARGET_Y, req.getWorldY());
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_FARMER_SPAWN_ID, (long) req.getFarmerSpawnId());
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_FARMER_MACHINE_ID, req.getRequesterMachineId());
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_JOB_STARTED_MS, System.currentTimeMillis());
                ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_ARRIVED);
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_EXCHANGE_ATTEMPTS, 0);
                ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_MULE_APPROVE_SENT);
                ctx.getPersistentData().remove(LogisticsCycleKeys.SWARM_MULE_FINALIZE_SENT);
                return BehaviorStatus.EXECUTED;
            }
        }
        return BehaviorStatus.SKIPPED;
    }

    @Override
    public long postDelayMs() {
        return 300L;
    }

    private List<LogisticsRequestEvent> pickEligibleRequests(IWorkflowContext ctx, LogisticsSettings cfg) {
        if (townModel == null) {
            return Collections.emptyList();
        }
        ITownSnapshot mine = townModel.snapshot(ctx.getMachineId()).orElse(null);
        if (mine == null) {
            return Collections.emptyList();
        }
        int myRegion = mine.getRegionId();
        GamePosition trainerPos = ctx.getGameModel().getTrainer().getPosition();
        WorldPoint from = trainerPos != null
                ? new WorldPoint((int) trainerPos.getX(), (int) trainerPos.getY(), (int) trainerPos.getZ())
                : null;
        long now = System.currentTimeMillis();
        List<LogisticsRequestEvent> list = new ArrayList<>();
        for (LogisticsRequestEvent r : swarmBus.openRequests()) {
            boolean sameRegion = r.getRegionId() == myRegion;
            if (!cfg.isCrossRegionEnabled() && !sameRegion) {
                continue;
            }
            if (!sameRegion && (from == null
                    || !routeFeasible(cfg, from, new WorldPoint(r.getWorldX(), r.getWorldY(), r.getWorldZ())))) {
                continue;
            }
            if (r.getRequesterMachineId().equals(ctx.getMachineId())) {
                continue;
            }
            if (r.getExpiresAtEpochMs() <= now) {
                continue;
            }
            list.add(r);
        }
        list.sort(Comparator
                .comparing((LogisticsRequestEvent r) -> r.getPriority().ordinal())
                .reversed()
                .thenComparingLong(LogisticsRequestEvent::getTimestampEpochMs));
        return list;
    }

    private boolean routeFeasible(LogisticsSettings cfg, WorldPoint from, WorldPoint to) {
        if (router == null) {
            return false;
        }
        java.util.Optional<TeleportRoute> route = router.route(from, to);
        if (!route.isPresent()) {
            return false;
        }
        TeleportRoute value = route.get();
        return value.size() <= cfg.getMaxHopCount() && value.getTotalCostGold() <= cfg.getMaxRouteCostGold();
    }
}
