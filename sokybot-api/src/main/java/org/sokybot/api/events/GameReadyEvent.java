package org.sokybot.api.events;

import java.util.List;

/**
 * Event fired when game is ready after teleport (opcode 0x3077).
 * Contains buff/skill cooldown information.
 */
public class GameReadyEvent implements IGameEvent {
    
    public record CooldownInfo(int id, int remainingMs) {}
    
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
