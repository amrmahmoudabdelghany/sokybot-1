package org.sokybot.gameevents.events.character;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when the character's basic data is updated.
 * Based on CHARACTER_DATA packet.
 */
public class CharacterDataEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final long currentExp;
    private final long skillPoints;
    private final int level;
    private final int strength;
    private final int intelligence;
    
    public CharacterDataEvent(String machineFullName, long currentExp, long skillPoints,
                             int level, int strength, int intelligence) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.currentExp = currentExp;
        this.skillPoints = skillPoints;
        this.level = level;
        this.strength = strength;
        this.intelligence = intelligence;
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
    
    public long getCurrentExp() {
        return currentExp;
    }
    
    public long getSkillPoints() {
        return skillPoints;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getStrength() {
        return strength;
    }
    
    public int getIntelligence() {
        return intelligence;
    }
}
