package org.sokybot.behaviors.hunting;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.sokybot.behaviors.combat.internal.CombatPackets;
import org.sokybot.behaviors.swarm.routing.RouteWalker;
import org.sokybot.combat.api.ICombatSnapshot;
import org.sokybot.combat.api.MonsterRef;
import org.sokybot.combat.projections.api.ICombatModel;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.behavior.IBehavior;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.swarm.api.HuntAbortedEvent;
import org.sokybot.swarm.api.HuntDispatchedEvent;
import org.sokybot.swarm.api.ISwarmEventBus;
import org.sokybot.topology.api.ITeleportGraph;
import org.sokybot.topology.api.ITeleportRouter;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.trade.coordination.api.SwarmRole;

@Component(service = IBehavior.class, scope = ServiceScope.PROTOTYPE)
public final class HuntDispatchBehavior implements IBehavior<HuntingSettings> {

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ISwarmEventBus swarmBus;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INavigator navigator;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITeleportRouter router;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ITeleportGraph graph;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile INpcInteractionFacade npcFacade;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile ICombatModel combatModel;

    @Reference
    private volatile HuntDispatchListener dispatchListener;

    @Override
    public String id() {
        return "hunt-dispatch";
    }

    @Override
    public int order() {
        return 7;
    }

    @Override
    public boolean appliesTo(String cycleId) {
        return HuntingCycleKeys.CYCLE_NAME.equals(cycleId);
    }

    @Override
    public Class<HuntingSettings> settingsType() {
        return HuntingSettings.class;
    }

    @Override
    public boolean applies(IWorkflowContext ctx, HuntingSettings cfg) {
        if (cfg == null || navigator == null) {
            return false;
        }
        SwarmRole role = cfg.getRole();
        if (role != SwarmRole.HUNTER && role != SwarmRole.BOTH) {
            return false;
        }
        return ctx.getPersistentData().get(HuntingCycleKeys.KEY_ACTIVE_HUNT_ID) != null
                || dispatchListener.hasPending(ctx.getMachineId());
    }

    @Override
    public BehaviorStatus execute(IWorkflowContext ctx, HuntingSettings cfg) {
        if (ctx.getPersistentData().get(HuntingCycleKeys.KEY_ACTIVE_HUNT_ID) == null) {
            captureIncomingDispatch(ctx);
            return BehaviorStatus.EXECUTED;
        }

        long dispatchedAt = longFrom(ctx.getPersistentData().get(HuntingCycleKeys.KEY_DISPATCHED_AT_MS));
        if (dispatchedAt > 0L && (System.currentTimeMillis() - dispatchedAt) > cfg.getHuntTimeoutMs()) {
            abortHunt(ctx, HuntAbortedEvent.Reason.TIMEOUT);
            return BehaviorStatus.EXECUTED;
        }

        WorldPoint target = readTarget(ctx);
        RouteWalker walker = new RouteWalker(navigator, router, graph, npcFacade, "hunt");
        RouteWalker.WalkResult result = walker.tick(ctx, target, cfg.getInteractionRadius());
        switch (result) {
            case ARRIVED:
                handoffToCombat(ctx, cfg, walker);
                break;
            case FAILED_NO_ROUTE:
                abortHunt(ctx, HuntAbortedEvent.Reason.NAVIGATION_FAILED);
                break;
            case FAILED_NAVIGATION:
                abortHunt(ctx, HuntAbortedEvent.Reason.NAVIGATION_FAILED);
                break;
            case FAILED_TIMEOUT:
                abortHunt(ctx, HuntAbortedEvent.Reason.TIMEOUT);
                break;
            case ROUTING:
            default:
                break;
        }
        return BehaviorStatus.EXECUTED;
    }

    private void captureIncomingDispatch(IWorkflowContext ctx) {
        HuntDispatchedEvent next = dispatchListener.poll(ctx.getMachineId());
        if (next == null) {
            return;
        }
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_ACTIVE_HUNT_ID, next.getHuntId());
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_TARGET_REFID, next.getTargetRefId());
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_TARGET_X, (int) next.getChosenPosition().getX());
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_TARGET_Y, (int) next.getChosenPosition().getY());
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_TARGET_Z, (int) next.getChosenPosition().getZ());
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_DISPATCHED_AT_MS, System.currentTimeMillis());
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_HUNTER_MACHINE_ID, next.getHunterMachineId());
        ctx.getPersistentData().put(HuntingCycleKeys.KEY_ARRIVAL_MISS_COUNT, 0);
    }

    private void handoffToCombat(IWorkflowContext ctx, HuntingSettings cfg, RouteWalker walker) {
        int targetRefId = intFrom(ctx.getPersistentData().get(HuntingCycleKeys.KEY_TARGET_REFID));
        Integer spawnId = findNearbyMatchingSpawn(ctx, targetRefId);
        if (spawnId == null) {
            int misses = intFrom(ctx.getPersistentData().get(HuntingCycleKeys.KEY_ARRIVAL_MISS_COUNT));
            ctx.getPersistentData().put(HuntingCycleKeys.KEY_ARRIVAL_MISS_COUNT, misses + 1);
            if (misses >= 5) {
                abortHunt(ctx, HuntAbortedEvent.Reason.TARGET_NOT_FOUND);
            }
            return;
        }
        CombatPackets.sendSelectEntity(ctx, spawnId);
        walker.clearState(ctx);
        clearHuntKeys(ctx);
    }

    private Integer findNearbyMatchingSpawn(IWorkflowContext ctx, int targetRefId) {
        ICombatModel model = combatModel;
        if (model == null) {
            return null;
        }
        ICombatSnapshot snap = model.snapshot(ctx.getMachineId()).orElse(null);
        if (snap == null) {
            return null;
        }
        for (MonsterRef m : snap.getNearbyMonsters()) {
            if (m.getRefObjId() == targetRefId) {
                return m.getEntityId();
            }
        }
        return null;
    }

    private void abortHunt(IWorkflowContext ctx, HuntAbortedEvent.Reason reason) {
        Object rawHunt = ctx.getPersistentData().get(HuntingCycleKeys.KEY_ACTIVE_HUNT_ID);
        String huntId = rawHunt instanceof String ? (String) rawHunt : null;
        int targetRefId = intFrom(ctx.getPersistentData().get(HuntingCycleKeys.KEY_TARGET_REFID));
        if (huntId != null && swarmBus != null) {
            swarmBus.publish(new HuntAbortedEvent(
                    ctx.getMachineId(),
                    System.currentTimeMillis(),
                    huntId,
                    huntId,
                    ctx.getMachineId(),
                    targetRefId,
                    reason));
        }
        clearHuntKeys(ctx);
    }

    private static void clearHuntKeys(IWorkflowContext ctx) {
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_ACTIVE_HUNT_ID);
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_TARGET_REFID);
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_TARGET_X);
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_TARGET_Y);
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_TARGET_Z);
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_DISPATCHED_AT_MS);
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_ARRIVAL_MISS_COUNT);
        ctx.getPersistentData().remove(HuntingCycleKeys.KEY_HUNTER_MACHINE_ID);
    }

    private static WorldPoint readTarget(IWorkflowContext ctx) {
        int x = intFrom(ctx.getPersistentData().get(HuntingCycleKeys.KEY_TARGET_X));
        int y = intFrom(ctx.getPersistentData().get(HuntingCycleKeys.KEY_TARGET_Y));
        int z = intFrom(ctx.getPersistentData().get(HuntingCycleKeys.KEY_TARGET_Z));
        return new WorldPoint(x, y, z);
    }

    private static int intFrom(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        return 0;
    }

    private static long longFrom(Object raw) {
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        return 0L;
    }

    @Override
    public long postDelayMs() {
        return 350L;
    }
}
