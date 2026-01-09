package org.sokybot.gameevents.events.character;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Fine-grained event emitted for each mastery detected during character data loading.
 * Provides full mastery details with name lookup from game data.
 */
public class CharacterMasteryLoadedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int masteryId;
    private final String masteryName;
    private final int masteryLevel;
    
    public CharacterMasteryLoadedEvent(String fullName, int masteryId, 
                                       String masteryName, int masteryLevel) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.masteryId = masteryId;
        this.masteryName = masteryName;
        this.masteryLevel = masteryLevel;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getMasteryId() { return masteryId; }
    public String getMasteryName() { return masteryName; }
    public int getMasteryLevel() { return masteryLevel; }
    
    @Override
    public String toString() {
        return "CharacterMasteryLoadedEvent{masteryId=" + masteryId + 
               ", masteryName='" + masteryName + "'" +
               ", masteryLevel=" + masteryLevel + "}";
    }
}

