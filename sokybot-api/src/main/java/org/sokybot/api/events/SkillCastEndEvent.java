package org.sokybot.api.events;

/**
 * Event fired when skill cast ends (completes or is interrupted).
 * Based on ServerOpcode.SKILL_CAST_ENDED (0xB071).
 */
public class SkillCastEndEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int casterId;
    private final int skillId;
    
    public SkillCastEndEvent(String machineFullName, int casterId, int skillId) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.casterId = casterId;
        this.skillId = skillId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getCasterId() { return casterId; }
    public int getSkillId() { return skillId; }
}
