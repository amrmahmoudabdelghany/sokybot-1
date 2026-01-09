package org.sokybot.api.events;

/**
 * Event emitted when teleport is completed (opcode 0x34B5 - GameResetComplete).
 */
public class TeleportCompleteEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    public TeleportCompleteEvent(String fullName) {
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
        return "TeleportCompleteEvent{}";
    }
}
