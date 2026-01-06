package org.sokybot.api.events;

/**
 * Event emitted when character joins a job (opcode 0xB0E1).
 */
public class JobJoinEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte jobType;
    private final byte jobLevel;
    private final long jobExp;
    
    public JobJoinEvent(String fullName, byte jobType, byte jobLevel, long jobExp) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.jobType = jobType;
        this.jobLevel = jobLevel;
        this.jobExp = jobExp;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getJobType() { return jobType; }
    public byte getJobLevel() { return jobLevel; }
    public long getJobExp() { return jobExp; }
    
    @Override
    public String toString() {
        return "JobJoinEvent{jobType=" + jobType + ", jobLevel=" + jobLevel + "}";
    }
}
