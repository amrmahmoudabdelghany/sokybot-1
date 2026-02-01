package org.sokybot.gameevents.events.teleport;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.dto.GamePosition;

/**
 * Event fired when character teleports to a new location.
 * Represents instant position changes (teleports, portals).
 */
public class TeleportEvent implements IGameEvent {

    private final String fullName;
    private final long timestamp;
    private final GamePosition fromPosition;
    private final GamePosition toPosition;
    private final byte teleportType; // 0 = normal, 1 = recall, etc.

    public TeleportEvent(String machineFullName, GamePosition fromPosition,
            GamePosition toPosition, byte teleportType) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
        this.teleportType = teleportType;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }

    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public GamePosition getFromPosition() {
        return fromPosition;
    }

    public GamePosition getToPosition() {
        return toPosition;
    }

    public byte getTeleportType() {
        return teleportType;
    }
}
