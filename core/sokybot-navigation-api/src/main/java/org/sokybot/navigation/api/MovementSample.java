package org.sokybot.navigation.api;

import java.util.Objects;

/** Single sampled character position at an instant (immutable). */
public final class MovementSample {

    private final long epochMillis;
    private final WorldPoint position;

    public MovementSample(long epochMillis, WorldPoint position) {
        this.epochMillis = epochMillis;
        this.position = Objects.requireNonNull(position, "position");
    }

    public long getEpochMillis() {
        return epochMillis;
    }

    public WorldPoint getPosition() {
        return position;
    }
}
