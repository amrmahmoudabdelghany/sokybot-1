package org.sokybot.api.events;

/**
 * Event fired when a skill level increases.
 * Based on TrainerHandler.skillLevelUp() pattern.
 */
public class SkillLevelUpEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final int skillId;
    private final String skillName;  // Name from skill lookup
    
    public SkillLevelUpEvent(String machineFullName, boolean success, int skillId, String skillName) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.skillId = skillId;
        this.skillName = skillName;
    }
    
    // Legacy constructor for backward compatibility
    public SkillLevelUpEvent(String machineFullName, boolean success, int skillId) {
        this(machineFullName, success, skillId, null);
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public int getSkillId() { return skillId; }
    public String getSkillName() { return skillName; }
}

