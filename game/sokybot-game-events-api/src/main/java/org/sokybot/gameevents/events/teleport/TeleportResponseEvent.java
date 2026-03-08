package org.sokybot.gameevents.events.teleport;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired for teleport response (opcode 0xB05A).
 * Response to a teleport request.
 */
public class TeleportResponseEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final int destinationId;
    
    public TeleportResponseEvent(String fullName, boolean success, int destinationId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.destinationId = destinationId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public int getDestinationId() { return destinationId; }
    
    @Override
    public String toString() {
        return String.format("TeleportResponseEvent[%s, success=%b, dest=%d]", 
            fullName, success, destinationId);
    }
}
