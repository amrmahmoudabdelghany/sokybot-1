package org.sokybot.behaviors.swarm.routing;

import java.util.Optional;

import org.sokybot.behaviors.logistics.LogisticsCycleKeys;
import org.sokybot.behaviors.navigation.TeleportJumpBehavior;
import org.sokybot.behaviors.navigation.TeleportJumpKeys;
import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.topology.api.ITeleportGraph;
import org.sokybot.topology.api.ITeleportRouter;
import org.sokybot.topology.api.TeleportEdge;
import org.sokybot.topology.api.TeleportRoute;
import org.sokybot.town.api.INpcInteractionFacade;

public final class RouteWalker {

    public enum WalkResult {
        ARRIVED,
        ROUTING,
        FAILED_NO_ROUTE,
        FAILED_NAVIGATION,
        FAILED_TIMEOUT
    }

    private final INavigator navigator;
    private final ITeleportRouter router;
    private final ITeleportGraph graph;
    private final INpcInteractionFacade npcFacade;
    private final String routeKeyPrefix;
    private volatile TeleportRoute cachedRoute;

    public RouteWalker(
            INavigator navigator,
            ITeleportRouter router,
            ITeleportGraph graph,
            INpcInteractionFacade npcFacade,
            String routeKeyPrefix) {
        this.navigator = navigator;
        this.router = router;
        this.graph = graph;
        this.npcFacade = npcFacade;
        this.routeKeyPrefix = routeKeyPrefix == null ? "route" : routeKeyPrefix.trim();
    }

    public WalkResult tick(IWorkflowContext ctx, WorldPoint target, float interactionRadius) {
        GamePosition my = ctx.getGameModel().getTrainer().getPosition();
        if (my == null) {
            return WalkResult.ROUTING;
        }
        if (withinRadius(my, target, interactionRadius)) {
            return WalkResult.ARRIVED;
        }

        TeleportRoute route = currentRoute(ctx, my, target);
        if (route == null) {
            return WalkResult.FAILED_NO_ROUTE;
        }
        if (route != null && !route.isEmpty()) {
            int cursor = intFrom(ctx.getPersistentData().get(keyRouteCursor()));
            if (cursor < route.size()) {
                TeleportEdge hop = route.hop(cursor);
                BehaviorStatus status = TeleportJumpBehavior.tick(
                        ctx, navigator, npcFacade, graph, hop, interactionRadius,
                        keyHopPhase(), keyHopStartedAt(), keyHopFuture());
                TeleportJumpBehavior.Phase phase = phaseFrom(ctx);
                if (phase == TeleportJumpBehavior.Phase.COMPLETE) {
                    ctx.getPersistentData().put(keyRouteCursor(), cursor + 1);
                    TeleportJumpBehavior.reset(ctx.getPersistentData(), keyHopPhase(), keyHopStartedAt(), keyHopFuture());
                } else if (phase == TeleportJumpBehavior.Phase.FAILED) {
                    return WalkResult.FAILED_NAVIGATION;
                }
                return status == BehaviorStatus.EXECUTED ? WalkResult.ROUTING : WalkResult.FAILED_NAVIGATION;
            }
        }
        try {
            navigator.walkTo(ctx, target);
            return WalkResult.ROUTING;
        } catch (NavigationException ex) {
            return WalkResult.FAILED_NAVIGATION;
        }
    }

    public void clearState(IWorkflowContext ctx) {
        cachedRoute = null;
        ctx.getPersistentData().remove(keyRouteCursor());
        ctx.getPersistentData().remove(keyRouteGeneration());
        ctx.getPersistentData().remove(keyRouteHopCount());
        ctx.getPersistentData().remove(keyHopPhase());
        ctx.getPersistentData().remove(keyHopStartedAt());
        ctx.getPersistentData().remove(keyHopFuture());
    }

    private TeleportRoute currentRoute(IWorkflowContext ctx, GamePosition from, WorldPoint to) {
        if (router == null || graph == null || npcFacade == null) {
            return null;
        }
        long generation = graph.generation();
        long storedGeneration = longFrom(ctx.getPersistentData().get(keyRouteGeneration()));
        boolean timeout = hopTimedOut(ctx);
        if (timeout) {
            cachedRoute = null;
        }
        if (cachedRoute == null || generation != storedGeneration || timeout) {
            Optional<TeleportRoute> recomputed = router.route(
                    new WorldPoint((int) from.getX(), (int) from.getY(), (int) from.getZ()),
                    to);
            cachedRoute = recomputed.orElse(null);
            if (cachedRoute != null) {
                ctx.getPersistentData().put(keyRouteCursor(), 0);
                ctx.getPersistentData().put(keyRouteGeneration(), generation);
                ctx.getPersistentData().put(keyRouteHopCount(), cachedRoute.size());
                TeleportJumpBehavior.reset(ctx.getPersistentData(), keyHopPhase(), keyHopStartedAt(), keyHopFuture());
            }
        }
        return cachedRoute;
    }

    private boolean hopTimedOut(IWorkflowContext ctx) {
        long started = longFrom(ctx.getPersistentData().get(keyHopStartedAt()));
        return started > 0L && System.currentTimeMillis() - started > TeleportJumpKeys.HOP_TIMEOUT_MS;
    }

    private String keyRouteCursor() {
        return "logistics".equals(routeKeyPrefix) ? LogisticsCycleKeys.KEY_ROUTE_CURSOR : routeKeyPrefix + "_route_cursor";
    }

    private String keyRouteGeneration() {
        return "logistics".equals(routeKeyPrefix) ? LogisticsCycleKeys.KEY_ROUTE_GENERATION : routeKeyPrefix + "_route_generation";
    }

    private String keyRouteHopCount() {
        return "logistics".equals(routeKeyPrefix) ? LogisticsCycleKeys.KEY_ROUTE_HOP_COUNT : routeKeyPrefix + "_route_hop_count";
    }

    private String keyHopPhase() {
        return "logistics".equals(routeKeyPrefix) ? TeleportJumpKeys.KEY_HOP_PHASE : routeKeyPrefix + "_hop_phase";
    }

    private String keyHopStartedAt() {
        return "logistics".equals(routeKeyPrefix) ? TeleportJumpKeys.KEY_HOP_STARTED_AT_MS : routeKeyPrefix + "_hop_started_at_ms";
    }

    private String keyHopFuture() {
        return "logistics".equals(routeKeyPrefix) ? TeleportJumpKeys.KEY_HOP_FUTURE : routeKeyPrefix + "_hop_future";
    }

    private static int intFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }

    private static long longFrom(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0L;
    }

    private static boolean withinRadius(GamePosition my, WorldPoint target, float radius) {
        float dx = target.getX() - my.getX();
        float dz = target.getZ() - my.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    private TeleportJumpBehavior.Phase phaseFrom(IWorkflowContext ctx) {
        Object raw = ctx.getPersistentData().get(keyHopPhase());
        if (raw instanceof String) {
            try {
                return TeleportJumpBehavior.Phase.valueOf((String) raw);
            } catch (IllegalArgumentException ignored) {
                return TeleportJumpBehavior.Phase.WALKING_TO_NPC;
            }
        }
        return TeleportJumpBehavior.Phase.WALKING_TO_NPC;
    }
}
