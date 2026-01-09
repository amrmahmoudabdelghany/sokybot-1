package org.sokybot.gameevents.events.job;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when character leaves a job (opcode 0xB0E2).
 */
public class JobLeaveEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    public JobLeaveEvent(String fullName) {
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
        return "JobLeaveEvent{}";
    }
}
