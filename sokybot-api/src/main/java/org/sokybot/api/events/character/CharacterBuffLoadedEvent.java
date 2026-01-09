package org.sokybot.api.events;

/**
 * Fine-grained event emitted for each active buff detected during character data loading.
 * Provides full buff details with name lookup from game data.
 */
public class CharacterBuffLoadedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int buffId;
    private final String buffName;
    private final int duration;          // Duration in seconds (0 = permanent)
    private final boolean isTransferable;
    
    public CharacterBuffLoadedEvent(String fullName, int buffId, 
                                    String buffName, int duration, boolean isTransferable) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.buffId = buffId;
        this.buffName = buffName;
        this.duration = duration;
        this.isTransferable = isTransferable;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getBuffId() { return buffId; }
    public String getBuffName() { return buffName; }
    public int getDuration() { return duration; }
    public boolean isTransferable() { return isTransferable; }
    public boolean isPermanent() { return duration == 0; }
    
    @Override
    public String toString() {
        return "CharacterBuffLoadedEvent{buffId=" + buffId + 
               ", buffName='" + buffName + "'" +
               ", duration=" + duration + "s" +
               ", isTransferable=" + isTransferable + "}";
    }
}

