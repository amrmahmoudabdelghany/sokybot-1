package org.sokybot.gameevents.events.skill;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when skill cast is confirmed (added to queue).
 * Based on ServerOpcode.SKILL_CAST_CONFIRM (0xB074).
 */
public class SkillCastConfirmEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte queuePosition;
    
    public SkillCastConfirmEvent(String machineFullName, boolean success, byte queuePosition) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.queuePosition = queuePosition;
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
    public byte getQueuePosition() { return queuePosition; }
}
