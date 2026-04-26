package org.sokybot.topology.api;

public final class TeleportEdge {

    private final int fromNpcRefId;
    private final int toNpcRefId;
    private final int destinationRefId;
    private final long goldCost;
    private final long expectedDurationMs;

    public TeleportEdge(int fromNpcRefId, int toNpcRefId, int destinationRefId, long goldCost, long expectedDurationMs) {
        this.fromNpcRefId = fromNpcRefId;
        this.toNpcRefId = toNpcRefId;
        this.destinationRefId = destinationRefId;
        this.goldCost = goldCost;
        this.expectedDurationMs = expectedDurationMs;
    }

    public int getFromNpcRefId() {
        return fromNpcRefId;
    }

    public int getToNpcRefId() {
        return toNpcRefId;
    }

    public int getDestinationRefId() {
        return destinationRefId;
    }

    public long getGoldCost() {
        return goldCost;
    }

    public long getExpectedDurationMs() {
        return expectedDurationMs;
    }
}
