package org.sokybot.api.events;

/**
 * Fine-grained event emitted for each skill detected during character data loading.
 * Provides full skill details with name lookup from game data.
 */
public class CharacterSkillLoadedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int skillId;
    private final String skillName;
    private final int skillLevel;
    private final boolean isEnabled;
    
    public CharacterSkillLoadedEvent(String fullName, int skillId, 
                                     String skillName, int skillLevel, boolean isEnabled) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.skillId = skillId;
        this.skillName = skillName;
        this.skillLevel = skillLevel;
        this.isEnabled = isEnabled;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getSkillId() { return skillId; }
    public String getSkillName() { return skillName; }
    public int getSkillLevel() { return skillLevel; }
    public boolean isEnabled() { return isEnabled; }
    
    @Override
    public String toString() {
        return "CharacterSkillLoadedEvent{skillId=" + skillId + 
               ", skillName='" + skillName + "'" +
               ", skillLevel=" + skillLevel +
               ", isEnabled=" + isEnabled + "}";
    }
}

