package org.sokybot.gameevents.events.entity;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when player deselects an entity (opcode 0xB04B).
 */
public class EntityDeselectedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    public EntityDeselectedEvent(String fullName) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "EntityDeselectedEvent{}";
    }
}
