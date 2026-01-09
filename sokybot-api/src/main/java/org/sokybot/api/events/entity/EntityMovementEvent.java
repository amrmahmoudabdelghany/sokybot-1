package org.sokybot.api.events;

import org.sokybot.persistence.entities.navmesh.Position;

/**
 * Event fired when an entity moves in the game world.
 * Contains movement destination and optionally current position.
 */
public class EntityMovementEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final boolean hasDestination;
    
    // Destination coordinates
    private final Position destination;
    
    // Current position (if hasOrigin flag is set)
    private final Position currentPosition;
    
    public EntityMovementEvent(String machineFullName, int entityId, boolean hasDestination,
                              Position destination, Position currentPosition) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.hasDestination = hasDestination;
        this.destination = destination;
        this.currentPosition = currentPosition;
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
    
    public int getEntityId() {
        return entityId;
    }
    
    public boolean hasDestination() {
        return hasDestination;
    }
    
    public Position getDestination() {
        return destination;
    }
    
    public Position getCurrentPosition() {
        return currentPosition;
    }
}
