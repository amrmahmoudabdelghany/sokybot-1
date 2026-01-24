package org.sokybot.gameevents.events.skill;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when skill points change.
 * Based on TrainerHandler.attackGainsUpdates() SP type.
 */
public class SkillPointsUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int newSkillPoints;
    
    public SkillPointsUpdateEvent(String machineFullName, int newSkillPoints) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.newSkillPoints = newSkillPoints;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getNewSkillPoints() { return newSkillPoints; }
}
