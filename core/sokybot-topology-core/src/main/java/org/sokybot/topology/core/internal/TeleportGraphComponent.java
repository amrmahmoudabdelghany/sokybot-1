package org.sokybot.topology.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.navigation.api.WorldPoint;
import org.sokybot.persistence.entities.TeleportDestinationEntity;
import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.topology.api.ITeleportGraph;
import org.sokybot.topology.api.TeleportEdge;
import org.sokybot.topology.api.TeleportNode;

@Component(immediate = true, service = ITeleportGraph.class)
public class TeleportGraphComponent implements ITeleportGraph {

    private static final long DEFAULT_EXPECTED_DURATION_MS = 8000L;
    private static final float ARRIVAL_NODE_MAX_DISTANCE = 200f;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IGameDataLookup gameData;

    private final AtomicReference<GraphSnapshot> snapshot = new AtomicReference<>(GraphSnapshot.empty());
    private final AtomicLong generation = new AtomicLong(0L);

    @Activate
    void activate() {
        rebuild();
    }

    public void rebuild() {
        IGameDataLookup lookup = gameData;
        if (lookup == null) {
            snapshot.set(GraphSnapshot.empty());
            generation.incrementAndGet();
            return;
        }

        Map<Integer, TeleportNode> nodes = new HashMap<>();
        List<TeleportEntity> teleports = new ArrayList<>();

        try (Stream<TeleportEntity> allTeleports = lookup.findAllTeleports()) {
            allTeleports.forEach(teleports::add);
        }

        for (TeleportEntity teleport : teleports) {
            int refId = teleport.getRefId();
            Optional<TeleportDestinationEntity> destination = lookup.findTeleportDestination(refId);
            if (!destination.isPresent()) {
                continue;
            }

            TeleportDestinationEntity destinationEntity = destination.get();
            WorldPoint point = new WorldPoint(destinationEntity.getX(), destinationEntity.getY(), destinationEntity.getZ());
            int packedSector = packSector(point);
            TeleportNode node = new TeleportNode(refId, safeName(teleport), point, packedSector);
            nodes.put(refId, node);
        }

        Map<Integer, List<TeleportEdge>> outEdges = new HashMap<>();
        Collection<TeleportNode> nodeValues = nodes.values();

        for (TeleportEntity teleport : teleports) {
            int fromRefId = teleport.getRefId();
            if (!nodes.containsKey(fromRefId)) {
                continue;
            }

            List<TeleportEdge> edges = new ArrayList<>();
            List<Integer> links = teleport.getLinks();
            if (links != null) {
                for (Integer destinationRefId : links) {
                    if (destinationRefId == null) {
                        continue;
                    }

                    Optional<TeleportDestinationEntity> destination = lookup.findTeleportDestination(destinationRefId);
                    if (!destination.isPresent()) {
                        continue;
                    }

                    TeleportDestinationEntity destinationEntity = destination.get();
                    WorldPoint destinationPoint = new WorldPoint(destinationEntity.getX(), destinationEntity.getY(),
                            destinationEntity.getZ());
                    Optional<TeleportNode> arrivalNode = nearestNodeIn(nodeValues, destinationPoint, ARRIVAL_NODE_MAX_DISTANCE);
                    if (!arrivalNode.isPresent()) {
                        continue;
                    }

                    TeleportEdge edge = new TeleportEdge(fromRefId, arrivalNode.get().getNpcRefId(), destinationRefId.intValue(),
                            destinationEntity.getGoldCost(), DEFAULT_EXPECTED_DURATION_MS);
                    edges.add(edge);
                }
            }

            outEdges.put(fromRefId, Collections.unmodifiableList(edges));
        }

        snapshot.set(new GraphSnapshot(nodes, outEdges));
        generation.incrementAndGet();
    }

    @Override
    public Collection<TeleportNode> nodes() {
        return snapshot.get().nodes.values();
    }

    @Override
    public List<TeleportEdge> edgesFrom(int npcRefId) {
        return snapshot.get().outEdges.getOrDefault(npcRefId, Collections.emptyList());
    }

    @Override
    public Optional<TeleportNode> nearestNode(WorldPoint pos) {
        return nearestNodeIn(snapshot.get().nodes.values(), pos, Float.MAX_VALUE);
    }

    @Override
    public Optional<TeleportNode> nearestNodeWithin(WorldPoint pos, float maxWorldUnits) {
        return nearestNodeIn(snapshot.get().nodes.values(), pos, maxWorldUnits);
    }

    @Override
    public long generation() {
        return generation.get();
    }

    private static Optional<TeleportNode> nearestNodeIn(Collection<TeleportNode> nodes, WorldPoint target, float maxDistance) {
        TeleportNode best = null;
        float bestDistance = maxDistance;
        for (TeleportNode node : nodes) {
            float distance = node.getPosition().distanceTo(target);
            if (distance <= bestDistance) {
                best = node;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private static int packSector(WorldPoint point) {
        int sectorX = Math.round(point.getX() / 192.0f);
        int sectorY = Math.round(point.getY() / 192.0f);
        return (sectorX << 16) | (sectorY & 0xFFFF);
    }

    private static String safeName(TeleportEntity teleport) {
        return teleport.getName() == null ? ("teleport-" + teleport.getRefId()) : teleport.getName();
    }

    private static final class GraphSnapshot {
        private final Map<Integer, TeleportNode> nodes;
        private final Map<Integer, List<TeleportEdge>> outEdges;

        private GraphSnapshot(Map<Integer, TeleportNode> nodes, Map<Integer, List<TeleportEdge>> outEdges) {
            this.nodes = Collections.unmodifiableMap(new HashMap<>(nodes));
            this.outEdges = Collections.unmodifiableMap(new HashMap<>(outEdges));
        }

        private static GraphSnapshot empty() {
            return new GraphSnapshot(Collections.emptyMap(), Collections.emptyMap());
        }
    }
}
