package org.sokybot.behaviors.swarm;

import org.sokybot.behaviors.swarm.routing.RouteWalker;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.logistics.LogisticsCycleKeys;
import org.sokybot.behaviors.logistics.LogisticsSettings;
import org.sokybot.behaviors.swarm.internal.SwarmJobMonitor;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.swarm.api.LogisticsAbortedEvent;
import org.sokybot.topology.api.ITeleportGraph;
import org.sokybot.topology.api.ITeleportRouter;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.trade.coordination.api.ITradeCoordinator;
import org.sokybot.trade.coordination.api.SwarmRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class MuleNavigateBehavior implements IBehavior<LogisticsSettings> {

    private static final Logger log = LoggerFactory.getLogger(MuleNavigateBehavior.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITradeCoordinator tradeCoordinator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITeleportRouter router;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITeleportGraph teleportGraph;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INpcInteractionFacade npcInteractionFacade;

    @Reference
    private volatile SwarmJobMonitor jobMonitor;

    @Override
    public String id() {
        return "swarm-mule-navigate";
    }

    @Override
    public int order() {
        return 9;
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
        if (cfg == null || swarmBus == null || navigator == null) {
            return false;
        }
        SwarmRole role = cfg.getRole();
        if (role != SwarmRole.MULE && role != SwarmRole.BOTH) {
            return false;
        }
        if (ctx.getPersistentData().get(LogisticsCycleKeys.KEY_ACTIVE_SWARM_REQUEST_ID) == null) {
            return false;
        }
        if (Boolean.TRUE.equals(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_ARRIVED))) {
            return false;
        }
        return tradeCoordinator == null || tradeCoordinator.getSwarmSession(ctx.getMachineId()).isEmpty();
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, LogisticsSettings cfg) {
        Object rawReq = ctx.getPersistentData().get(LogisticsCycleKeys.KEY_ACTIVE_SWARM_REQUEST_ID);
        if (!(rawReq instanceof String)) {
            return BehaviorStatus.SKIPPED;
        }
        String reqId = (String) rawReq;

        Object fs = ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_FARMER_SPAWN_ID);
        int farmerSpawn = fs instanceof Number ? ((Number) fs).intValue() : 0;
        if (ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_JOB_MONITOR_REGISTERED) == null) {
            jobMonitor.registerJob(ctx.getMachineId(), reqId, farmerSpawn);
            ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_JOB_MONITOR_REGISTERED, Boolean.TRUE);
        }

        LogisticsAbortedEvent.Reason abort = jobMonitor.pollAbortReason(ctx.getMachineId());
        if (abort != null) {
            abortJob(ctx, cfg, reqId, abort);
            return BehaviorStatus.EXECUTED;
        }

        Object started = ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_JOB_STARTED_MS);
        long t0 = started instanceof Number ? ((Number) started).longValue() : 0L;
        if (t0 > 0L && System.currentTimeMillis() - t0 > cfg.getJobTimeoutMs()) {
            abortJob(ctx, cfg, reqId, LogisticsAbortedEvent.Reason.TIMEOUT);
            return BehaviorStatus.EXECUTED;
        }

        int x = intFrom(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_TARGET_X));
        int z = intFrom(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_TARGET_Z));
        int y = intFrom(ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_TARGET_Y));

        GamePosition my = ctx.getGameModel().getTrainer().getPosition();
        if (my == null) {
            return BehaviorStatus.SKIPPED;
        }
        WorldPoint destination = new WorldPoint(x, y, z);
        float radius = resolveInteractionRadius(cfg);
        if (withinRadius(my, destination, radius)) {
            ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_ARRIVED, Boolean.TRUE);
            return BehaviorStatus.EXECUTED;
        }

        if (cfg.isCrossRegionEnabled()) {
            RouteWalker walker = new RouteWalker(navigator, router, teleportGraph, npcInteractionFacade, "logistics");
            RouteWalker.WalkResult walkResult = walker.tick(ctx, destination, radius);
            if (walkResult == RouteWalker.WalkResult.ARRIVED) {
                ctx.getPersistentData().put(LogisticsCycleKeys.SWARM_ARRIVED, Boolean.TRUE);
                return BehaviorStatus.EXECUTED;
            }
            if (walkResult == RouteWalker.WalkResult.FAILED_NAVIGATION
                    || walkResult == RouteWalker.WalkResult.FAILED_TIMEOUT) {
                abortJob(ctx, cfg, reqId, LogisticsAbortedEvent.Reason.NAVIGATION_FAILED);
                return BehaviorStatus.EXECUTED;
            }
            if (walkResult == RouteWalker.WalkResult.ROUTING) {
                return BehaviorStatus.EXECUTED;
            }
        }
        try {
            navigator.walkTo(ctx, destination);
        } catch (NavigationException e) {
            log.warn("Swarm navigation failed for {}: {}", ctx.getMachineId(), e.getMessage());
            abortJob(ctx, cfg, reqId, LogisticsAbortedEvent.Reason.NAVIGATION_FAILED);
        }
        return BehaviorStatus.EXECUTED;
    }

    private void abortJob(IWorkflowContext ctx, LogisticsSettings cfg, String reqId, LogisticsAbortedEvent.Reason reason) {
        Object fm = ctx.getPersistentData().get(LogisticsCycleKeys.SWARM_FARMER_MACHINE_ID);
        String farmerMachine = fm instanceof String ? (String) fm : ctx.getMachineId();
        swarmBus.publish(new LogisticsAbortedEvent(farmerMachine, System.currentTimeMillis(), reqId, reason));
        swarmBus.releaseClaim(reqId);
        if (tradeCoordinator != null) {
            tradeCoordinator.closeSwarmSession(ctx.getMachineId(), org.sokybot.trade.coordination.api.SwarmSessionResult.ABORTED);
        }
        clearSwarmKeys(ctx);
        jobMonitor.clearJob(ctx.getMachineId());
    }

    private static void clearSwarmKeys(IWorkflowContext ctx) {
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
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_ROUTE_CURSOR);
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_ROUTE_GENERATION);
        ctx.getPersistentData().remove(LogisticsCycleKeys.KEY_ROUTE_HOP_COUNT);
        ctx.getPersistentData().remove("swarm_route_hop_phase");
        ctx.getPersistentData().remove("swarm_route_hop_started_at_ms");
        ctx.getPersistentData().remove("swarm_route_hop_future");
    }

    private static int intFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }

    @Override
    public long postDelayMs() {
        return 400L;
    }

    private static float resolveInteractionRadius(LogisticsSettings cfg) {
        if (cfg.getInteractionRadius() > 0f) {
            return cfg.getInteractionRadius();
        }
        return (float) Math.sqrt(Math.max(1, cfg.getInteractionRadiusSq()));
    }

    private static boolean withinRadius(GamePosition my, WorldPoint target, float radius) {
        float dx = target.getX() - my.getX();
        float dz = target.getZ() - my.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

}
