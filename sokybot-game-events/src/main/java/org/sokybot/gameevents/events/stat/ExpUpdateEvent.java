package org.sokybot.gameevents.events.stat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when experience points change.
 * Based on EXP_UPDATE packet.
 */
public class ExpUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final long expGained;
    private final long totalExp;
    private final boolean levelUp;
    
    public ExpUpdateEvent(String machineFullName, long expGained, long totalExp, boolean levelUp) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.expGained = expGained;
        this.totalExp = totalExp;
        this.levelUp = levelUp;
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
    
    public long getExpGained() {
        return expGained;
    }
    
    public long getTotalExp() {
        return totalExp;
    }
    
    public boolean isLevelUp() {
        return levelUp;
    }
}
