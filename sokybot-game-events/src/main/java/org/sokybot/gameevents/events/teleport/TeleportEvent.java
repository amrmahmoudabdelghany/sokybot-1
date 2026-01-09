package org.sokybot.gameevents.events.teleport;
import org.sokybot.gameevents.events.core.IGameEvent;

import org.sokybot.persistence.entities.navmesh.Position;

/**
 * Event fired when character teleports to a new location.
 * Represents instant position changes (teleports, portals).
 */
public class TeleportEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final Position fromPosition;
    private final Position toPosition;
    private final byte teleportType; // 0 = normal, 1 = recall, etc.
    
    public TeleportEvent(String machineFullName, Position fromPosition, 
                        Position toPosition, byte teleportType) {
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
    
    public Position getFromPosition() {
        return fromPosition;
    }
    
    public Position getToPosition() {
        return toPosition;
    }
    
    public byte getTeleportType() {
        return teleportType;
    }
}
