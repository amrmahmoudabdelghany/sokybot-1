package org.sokybot.api.events;

/**
 * Event fired when character stat points are updated (opcode 0xB050/0xB051).
 * Covers STR and INT stat point distribution responses.
 */
public class StatPointsUpdateEvent implements IGameEvent {
    
    public enum StatType {
        STRENGTH,    // STR stat (0xB050)
        INTELLIGENCE // INT stat (0xB051)
    }
    
    private final String fullName;
    private final long timestamp;
    private final StatType statType;
    private final boolean success;
    private final int remainingPoints;  // Remaining unallocated stat points
    
    public StatPointsUpdateEvent(String fullName, StatType statType, 
                                 boolean success, int remainingPoints) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.statType = statType;
        this.success = success;
        this.remainingPoints = remainingPoints;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public StatType getStatType() { return statType; }
    public boolean isSuccess() { return success; }
    public int getRemainingPoints() { return remainingPoints; }
    
    @Override
    public String toString() {
        return String.format("StatPointsUpdateEvent[%s, stat=%s, success=%b, remaining=%d]", 
            fullName, statType, success, remainingPoints);
    }
}
