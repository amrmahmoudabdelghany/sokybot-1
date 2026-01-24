package org.sokybot.gameevents.events.skill;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when a skill cast begins.
 * Based on SKILL_CAST_STARTED packet (0x3844).
 */
public class SkillCastEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final Integer skillId;
    private final String skillName;  // Name from skill lookup
    private final Integer casterId;
    private final Integer targetId;
    
    public SkillCastEvent(String machineFullName, boolean success, 
                         Integer skillId, String skillName, Integer casterId, Integer targetId) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.skillId = skillId;
        this.skillName = skillName;
        this.casterId = casterId;
        this.targetId = targetId;
    }
    
    // Legacy constructor for backward compatibility
    public SkillCastEvent(String machineFullName, boolean success, 
                         Integer skillId, Integer casterId, Integer targetId) {
        this(machineFullName, success, skillId, null, casterId, targetId);
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public String getGroupName() {
        return fullName.split("\\.")[0];
    }
    
    @Override
    public String getMachineName() {
        return fullName.split("\\.")[1];
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Integer getSkillId() {
        return skillId;
    }
    
    public String getSkillName() {
        return skillName;
    }
    
    public Integer getCasterId() {
        return casterId;
    }
    
    public Integer getTargetId() {
        return targetId;
    }
}

