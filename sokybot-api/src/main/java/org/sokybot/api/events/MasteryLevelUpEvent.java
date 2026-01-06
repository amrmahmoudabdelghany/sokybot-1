package org.sokybot.api.events;

/**
 * Event fired when a mastery level increases.
 * Based on TrainerHandler.masteryLevelUp() pattern.
 */
public class MasteryLevelUpEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final int masteryId;
    private final int newLevel;
    
    public MasteryLevelUpEvent(String machineFullName, boolean success, 
                              int masteryId, int newLevel) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.masteryId = masteryId;
        this.newLevel = newLevel;
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
    public int getMasteryId() { return masteryId; }
    public int getNewLevel() { return newLevel; }
}
