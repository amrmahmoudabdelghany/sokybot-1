package org.sokybot.gameevents.events.character;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when character dies.
 * Critical event for bot logic and safety.
 */
public class CharacterDeathEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final Integer killerId; // Can be null if environmental death
    
    public CharacterDeathEvent(String machineFullName, Integer killerId) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.killerId = killerId;
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
    
    public Integer getKillerId() {
        return killerId;
    }
}
