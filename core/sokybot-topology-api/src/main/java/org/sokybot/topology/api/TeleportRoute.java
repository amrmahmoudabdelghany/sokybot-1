package org.sokybot.topology.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

public final class TeleportRoute {

    private final List<TeleportEdge> hops;
    private final WorldPoint finalDestination;
    private final int finalNodeRefId;
    private final long totalCostGold;
    private final long estimatedDurationMs;

    public TeleportRoute(List<TeleportEdge> hops, WorldPoint finalDestination, int finalNodeRefId, long totalCostGold,
            long estimatedDurationMs) {
        this.hops = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(hops, "hops")));
        this.finalDestination = Objects.requireNonNull(finalDestination, "finalDestination");
        this.finalNodeRefId = finalNodeRefId;
        this.totalCostGold = totalCostGold;
        this.estimatedDurationMs = estimatedDurationMs;
    }

    public boolean isEmpty() {
        return hops.isEmpty();
    }

    public int size() {
        return hops.size();
    }

    public TeleportEdge hop(int index) {
        return hops.get(index);
    }

    public List<TeleportEdge> getHops() {
        return hops;
    }

    public WorldPoint getFinalDestination() {
        return finalDestination;
    }

    public int getFinalNodeRefId() {
        return finalNodeRefId;
    }

    public long getTotalCostGold() {
        return totalCostGold;
    }

    public long getEstimatedDurationMs() {
        return estimatedDurationMs;
    }
}
