package org.sokybot.api.events;

/**
 * Event emitted when player successfully logs out (opcode 0x300A).
 */
public class LogoutEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    public LogoutEvent(String fullName) {
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
        return "LogoutEvent{}";
    }
}
