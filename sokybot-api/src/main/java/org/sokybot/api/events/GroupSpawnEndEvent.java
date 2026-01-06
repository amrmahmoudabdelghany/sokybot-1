package org.sokybot.api.events;

/**
 * Event emitted when group spawn data ends (opcode 0x3018).
 * This signals the end of a batch spawn operation.
 */
public class GroupSpawnEndEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    public GroupSpawnEndEvent(String fullName) {
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
        return "GroupSpawnEndEvent{}";
    }
}
