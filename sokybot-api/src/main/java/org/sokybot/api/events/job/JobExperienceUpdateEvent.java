package org.sokybot.api.events;

/**
 * Event fired when job experience is updated (opcode 0x30E6).
 * Covers trader, hunter, and thief job experience.
 */
public class JobExperienceUpdateEvent implements IGameEvent {
    
    public enum JobType {
        NONE,
        TRADER,
        HUNTER,
        THIEF
    }
    
    private final String fullName;
    private final long timestamp;
    private final JobType jobType;
    private final int jobLevel;
    private final long experience;
    
    public JobExperienceUpdateEvent(String fullName, JobType jobType, int jobLevel, long experience) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.jobType = jobType;
        this.jobLevel = jobLevel;
        this.experience = experience;
    }
    
    public JobExperienceUpdateEvent(String fullName, byte jobTypeCode, int jobLevel, long experience) {
        this(fullName, mapJobType(jobTypeCode), jobLevel, experience);
    }
    
    private static JobType mapJobType(byte code) {
        switch (code) {
            case 1: return JobType.TRADER;
            case 2: return JobType.HUNTER;
            case 3: return JobType.THIEF;
            default: return JobType.NONE;
        }
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public JobType getJobType() { return jobType; }
    public int getJobLevel() { return jobLevel; }
    public long getExperience() { return experience; }
    
    @Override
    public String toString() {
        return String.format("JobExperienceUpdateEvent[%s, type=%s, level=%d, exp=%d]", 
            fullName, jobType, jobLevel, experience);
    }
}
