package org.sokybot.gameevents.events.session;
import org.sokybot.gameevents.events.core.IGameEvent;

import java.util.List;

/**
 * Event fired when game is ready after teleport (opcode 0x3077).
 * Contains buff/skill cooldown information.
 */
public class GameReadyEvent implements IGameEvent {
    
    public static class CooldownInfo {
        private final int id;
        private final int remainingMs;
        
        public CooldownInfo(int id, int remainingMs) {
            this.id = id;
            this.remainingMs = remainingMs;
        }
        
        public int id() { return id; }
        public int remainingMs() { return remainingMs; }
    }
    
    private final String fullName;
    private final long timestamp;
    private final List<CooldownInfo> itemCooldowns;
    private final List<CooldownInfo> skillCooldowns;
    
    public GameReadyEvent(String fullName, List<CooldownInfo> itemCooldowns, 
                          List<CooldownInfo> skillCooldowns) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.itemCooldowns = itemCooldowns;
        this.skillCooldowns = skillCooldowns;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public List<CooldownInfo> getItemCooldowns() { return itemCooldowns; }
    public List<CooldownInfo> getSkillCooldowns() { return skillCooldowns; }
    
    @Override
    public String toString() {
        return String.format("GameReadyEvent[%s, items=%d, skills=%d]", 
            fullName, itemCooldowns.size(), skillCooldowns.size());
    }
}
