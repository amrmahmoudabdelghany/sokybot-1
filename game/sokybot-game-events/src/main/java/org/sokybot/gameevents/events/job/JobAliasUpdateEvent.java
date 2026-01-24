package org.sokybot.gameevents.events.job;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when job alias is updated (opcode 0xB0E3).
 * Players can set custom job names.
 */
public class JobAliasUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final String jobAlias;
    
    public JobAliasUpdateEvent(String fullName, boolean success, String jobAlias) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.jobAlias = jobAlias;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public String getJobAlias() { return jobAlias; }
    
    @Override
    public String toString() {
        return String.format("JobAliasUpdateEvent[%s, success=%b, alias=%s]", 
            fullName, success, jobAlias);
    }
}
