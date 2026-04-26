package org.sokybot.topology.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.topology.api.ITeleportGraph;
import org.sokybot.topology.api.ITeleportRouter;
import org.sokybot.topology.api.TeleportEdge;
import org.sokybot.topology.api.TeleportNode;
import org.sokybot.topology.api.TeleportRoute;

@Component(immediate = true, service = ITeleportRouter.class)
public class DijkstraTeleportRouter implements ITeleportRouter {

    private static final float WALK_MS_PER_UNIT = 12.0f;
    private static final float MAX_WALK_TO_TELEPORTER = 1500f;
    private static final long GOLD_SOFT_PENALTY_MULTIPLIER = 1L;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ITeleportGraph graph;

    @Override
    public Optional<TeleportRoute> route(WorldPoint from, WorldPoint to) {
        Optional<TeleportNode> origin = graph.nearestNodeWithin(from, MAX_WALK_TO_TELEPORTER);
        Optional<TeleportNode> destination = graph.nearestNodeWithin(to, MAX_WALK_TO_TELEPORTER);

        if (origin.isPresent() && destination.isPresent()
                && origin.get().getNpcRefId() == destination.get().getNpcRefId()) {
            long walkOnlyDuration = walkMs(from.distanceTo(to));
            return Optional.of(new TeleportRoute(Collections.emptyList(), to, destination.get().getNpcRefId(), 0L,
                    walkOnlyDuration));
        }

        if (!origin.isPresent() || !destination.isPresent()) {
            return Optional.empty();
        }

        int sourceRefId = origin.get().getNpcRefId();
        int targetRefId = destination.get().getNpcRefId();

        Map<Integer, Long> bestCost = new HashMap<>();
        Map<Integer, TeleportEdge> cameVia = new HashMap<>();
        PriorityQueue<NodeCost> queue = new PriorityQueue<>(Comparator.comparingLong(NodeCost::getCost));

        long entryWalkCost = walkMs(from.distanceTo(origin.get().getPosition()));
        bestCost.put(sourceRefId, entryWalkCost);
        queue.offer(new NodeCost(sourceRefId, entryWalkCost));

        while (!queue.isEmpty()) {
            NodeCost current = queue.poll();
            long knownCost = bestCost.getOrDefault(current.nodeRefId, Long.MAX_VALUE);
            if (current.cost > knownCost) {
                continue;
            }
            if (current.nodeRefId == targetRefId) {
                break;
            }

            List<TeleportEdge> edges = graph.edgesFrom(current.nodeRefId);
            for (TeleportEdge edge : edges) {
                long edgeCost = edge.getExpectedDurationMs() + (edge.getGoldCost() * GOLD_SOFT_PENALTY_MULTIPLIER);
                long candidate = current.cost + edgeCost;
                int toRefId = edge.getToNpcRefId();
                long previous = bestCost.getOrDefault(toRefId, Long.MAX_VALUE);
                if (candidate < previous) {
                    bestCost.put(toRefId, candidate);
                    cameVia.put(toRefId, edge);
                    queue.offer(new NodeCost(toRefId, candidate));
                }
            }
        }

        Long teleportCost = bestCost.get(targetRefId);
        if (teleportCost == null) {
            return Optional.empty();
        }

        LinkedList<TeleportEdge> hops = new LinkedList<>();
        int cursor = targetRefId;
        while (cameVia.containsKey(cursor)) {
            TeleportEdge edge = cameVia.get(cursor);
            hops.addFirst(edge);
            cursor = edge.getFromNpcRefId();
        }

        List<TeleportEdge> immutableHops = Collections.unmodifiableList(new ArrayList<>(hops));
        long totalGold = immutableHops.stream().mapToLong(TeleportEdge::getGoldCost).sum();
        long exitWalkCost = walkMs(destination.get().getPosition().distanceTo(to));
        long estimatedDurationMs = teleportCost + exitWalkCost;
        return Optional.of(new TeleportRoute(immutableHops, to, targetRefId, totalGold, estimatedDurationMs));
    }

    private static long walkMs(float distance) {
        return Math.max(0L, (long) (distance * WALK_MS_PER_UNIT));
    }

    private static final class NodeCost {
        private final int nodeRefId;
        private final long cost;

        private NodeCost(int nodeRefId, long cost) {
            this.nodeRefId = nodeRefId;
            this.cost = cost;
        }

        private long getCost() {
            return cost;
        }
    }
}
