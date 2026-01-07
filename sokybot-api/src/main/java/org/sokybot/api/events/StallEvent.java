package org.sokybot.api.events;

/**
 * Event for player stall (street vendor) system events.
 * Covers create, destroy, and update operations (opcodes 0x30B7-0x30BB).
 */
public class StallEvent implements IGameEvent {
    
    public enum StallEventType {
        ACTION,       // 0x30B7 - Stall action (buy, etc.)
        CREATED,      // 0x30B8 - Stall was created
        DESTROYED,    // 0x30B9 - Stall was destroyed
        NAME_CHANGED  // 0x30BB - Stall name changed
    }
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final StallEventType eventType;
    private final String stallName;  // Optional, for NAME_CHANGED
    
    public StallEvent(String fullName, int entityId, StallEventType eventType, String stallName) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.eventType = eventType;
        this.stallName = stallName;
    }
    
    public StallEvent(String fullName, int entityId, StallEventType eventType) {
        this(fullName, entityId, eventType, null);
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public StallEventType getEventType() { return eventType; }
    public String getStallName() { return stallName; }
    
    @Override
    public String toString() {
        String nameInfo = stallName != null ? ", name=" + stallName : "";
        return String.format("StallEvent[%s, entity=%d, type=%s%s]", 
            fullName, entityId, eventType, nameInfo);
    }
}
