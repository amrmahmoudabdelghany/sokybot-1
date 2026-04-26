package org.sokybot.behaviors.navigation;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.sokybot.engine.api.behavior.BehaviorStatus;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.topology.api.ITeleportGraph;
import org.sokybot.topology.api.TeleportEdge;
import org.sokybot.topology.api.TeleportNode;
import org.sokybot.town.api.INpcInteractionFacade;
import org.sokybot.town.api.NpcInteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TeleportJumpBehavior {

    private static final Logger log = LoggerFactory.getLogger(TeleportJumpBehavior.class);

    public enum Phase {
        WALKING_TO_NPC,
        TALKING,
        AWAITING_RESPONSE,
        AWAITING_LOAD,
        COMPLETE,
        FAILED
    }

    private TeleportJumpBehavior() {
    }

    public static BehaviorStatus tick(
            IWorkflowContext ctx,
            INavigator navigator,
            INpcInteractionFacade npcInteractionFacade,
            ITeleportGraph teleportGraph,
            TeleportEdge hop,
            float interactionRadius) {
        Map<String, Object> data = ctx.getPersistentData();
        Phase phase = phaseFrom(data.get(TeleportJumpKeys.KEY_HOP_PHASE));

        switch (phase) {
            case WALKING_TO_NPC:
                Optional<TeleportNode> fromNode = findNode(teleportGraph, hop.getFromNpcRefId());
                if (!fromNode.isPresent()) {
                    data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.FAILED.name());
                    return BehaviorStatus.EXECUTED;
                }
                if (isWithin(ctx, fromNode.get().getPosition(), interactionRadius)) {
                    data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.TALKING.name());
                    return BehaviorStatus.EXECUTED;
                }
                try {
                    navigator.walkTo(ctx, fromNode.get().getPosition());
                    return BehaviorStatus.EXECUTED;
                } catch (NavigationException ex) {
                    log.debug("Teleport hop walk failed: {}", ex.toString());
                    data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.FAILED.name());
                    return BehaviorStatus.EXECUTED;
                }
            case TALKING:
                CompletableFuture<NpcInteractionResult> cf = npcInteractionFacade
                        .teleport(ctx, hop.getFromNpcRefId(), hop.getDestinationRefId());
                data.put(TeleportJumpKeys.KEY_HOP_FUTURE, cf);
                data.put(TeleportJumpKeys.KEY_HOP_STARTED_AT_MS, System.currentTimeMillis());
                data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.AWAITING_RESPONSE.name());
                return BehaviorStatus.EXECUTED;
            case AWAITING_RESPONSE:
                CompletableFuture<NpcInteractionResult> pending = asFuture(data.get(TeleportJumpKeys.KEY_HOP_FUTURE));
                if (pending == null) {
                    data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.FAILED.name());
                    return BehaviorStatus.EXECUTED;
                }
                if (!pending.isDone()) {
                    return BehaviorStatus.EXECUTED;
                }
                NpcInteractionResult result = safeNow(pending);
                if (result == null || !result.isSuccess()) {
                    data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.FAILED.name());
                    return BehaviorStatus.EXECUTED;
                }
                data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.AWAITING_LOAD.name());
                return BehaviorStatus.EXECUTED;
            case AWAITING_LOAD:
                CompletableFuture<NpcInteractionResult> loading = asFuture(data.get(TeleportJumpKeys.KEY_HOP_FUTURE));
                if (loading == null || !loading.isDone()) {
                    return BehaviorStatus.EXECUTED;
                }
                NpcInteractionResult loaded = safeNow(loading);
                data.put(TeleportJumpKeys.KEY_HOP_PHASE, loaded != null && loaded.isSuccess()
                        ? Phase.COMPLETE.name()
                        : Phase.FAILED.name());
                return BehaviorStatus.EXECUTED;
            case COMPLETE:
            case FAILED:
            default:
                return BehaviorStatus.EXECUTED;
        }
    }

    public static void reset(Map<String, Object> data) {
        data.remove(TeleportJumpKeys.KEY_HOP_FUTURE);
        data.remove(TeleportJumpKeys.KEY_HOP_STARTED_AT_MS);
        data.put(TeleportJumpKeys.KEY_HOP_PHASE, Phase.WALKING_TO_NPC.name());
    }

    private static boolean isWithin(IWorkflowContext ctx, WorldPoint point, float radius) {
        GamePosition my = ctx.getGameModel().getTrainer().getPosition();
        if (my == null) {
            return false;
        }
        float dx = point.getX() - my.getX();
        float dz = point.getZ() - my.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    private static Optional<TeleportNode> findNode(ITeleportGraph graph, int npcRefId) {
        for (TeleportNode node : graph.nodes()) {
            if (node.getNpcRefId() == npcRefId) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    private static Phase phaseFrom(Object raw) {
        if (raw instanceof String) {
            try {
                return Phase.valueOf((String) raw);
            } catch (IllegalArgumentException ignored) {
                return Phase.WALKING_TO_NPC;
            }
        }
        return Phase.WALKING_TO_NPC;
    }

    @SuppressWarnings("unchecked")
    private static CompletableFuture<NpcInteractionResult> asFuture(Object raw) {
        if (raw instanceof CompletableFuture<?>) {
            return (CompletableFuture<NpcInteractionResult>) raw;
        }
        return null;
    }

    private static NpcInteractionResult safeNow(CompletableFuture<NpcInteractionResult> future) {
        try {
            return future.join();
        } catch (CompletionException ex) {
            return null;
        }
    }
}
