package org.sokybot.gameevents.events.stat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when Hwan level updates (opcode 0x30DF).
 * Hwan is a buff system in Silkroad Online.
 */
public class HwanLevelUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final byte hwanLevel;     // Current Hwan level (0-5)
    private final int hwanProgress;   // Progress toward next level
    
    public HwanLevelUpdateEvent(String fullName, int entityId, 
                                byte hwanLevel, int hwanProgress) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.hwanLevel = hwanLevel;
        this.hwanProgress = hwanProgress;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public byte getHwanLevel() { return hwanLevel; }
    public int getHwanProgress() { return hwanProgress; }
    
    @Override
    public String toString() {
        return String.format("HwanLevelUpdateEvent[%s, entity=%d, level=%d, progress=%d]", 
            fullName, entityId, hwanLevel, hwanProgress);
    }
}
