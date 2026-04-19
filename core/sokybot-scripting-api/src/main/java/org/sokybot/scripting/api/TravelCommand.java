package org.sokybot.scripting.api;

import java.util.Objects;

import org.sokybot.navigation.api.WorldPoint;

/**
 * One immutable step in a travel script.
 */
public final class TravelCommand {

    private final TravelCommandKind kind;
    private final WorldPoint point;
    private final int npcRefId;
    private final int destinationRefId;
    private final long waitMs;
    private final String message;
    private final int sourceLine;

    private TravelCommand(TravelCommandKind kind, WorldPoint point, int npcRefId, int destinationRefId,
            long waitMs, String message, int sourceLine) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.point = point;
        this.npcRefId = npcRefId;
        this.destinationRefId = destinationRefId;
        this.waitMs = waitMs;
        this.message = message;
        this.sourceLine = sourceLine;
    }

    public TravelCommandKind getKind() {
        return kind;
    }

    /** Non-null for {@link TravelCommandKind#WALK}, {@link TravelCommandKind#PORTAL}. */
    public WorldPoint getPoint() {
        return point;
    }

    /** Used for {@link TravelCommandKind#TELEPORT}. */
    public int getNpcRefId() {
        return npcRefId;
    }

    /** Used for {@link TravelCommandKind#TELEPORT}. */
    public int getDestinationRefId() {
        return destinationRefId;
    }

    /** Used for {@link TravelCommandKind#WAIT}. */
    public long getWaitMs() {
        return waitMs;
    }

    /** Used for {@link TravelCommandKind#LOG}. */
    public String getMessage() {
        return message;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public static TravelCommand walk(WorldPoint p, int sourceLine) {
        Objects.requireNonNull(p, "point");
        return new TravelCommand(TravelCommandKind.WALK, p, 0, 0, 0L, null, sourceLine);
    }

    public static TravelCommand teleport(int npcRefId, int destinationRefId, int sourceLine) {
        return new TravelCommand(TravelCommandKind.TELEPORT, null, npcRefId, destinationRefId, 0L, null,
                sourceLine);
    }

    public static TravelCommand portal(WorldPoint p, int sourceLine) {
        Objects.requireNonNull(p, "point");
        return new TravelCommand(TravelCommandKind.PORTAL, p, 0, 0, 0L, null, sourceLine);
    }

    public static TravelCommand waitMillis(long millis, int sourceLine) {
        return new TravelCommand(TravelCommandKind.WAIT, null, 0, 0, millis, null, sourceLine);
    }

    public static TravelCommand log(String text, int sourceLine) {
        return new TravelCommand(TravelCommandKind.LOG, null, 0, 0, 0L, Objects.requireNonNull(text, "message"),
                sourceLine);
    }
}
