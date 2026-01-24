package org.sokybot.gameevents.events.stat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when an entity's life/motion/body/pvp/battle state changes.
 * This is different from HP/MP updates - this is state type updates from opcode 0x30BF.
 */
public class LifeStateUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int uniqueId;
    private final byte updateType;
    private final byte stateValue;
    
    public LifeStateUpdateEvent(String fullName, int uniqueId, byte updateType, byte stateValue) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.uniqueId = uniqueId;
        this.updateType = updateType;
        this.stateValue = stateValue;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getUniqueId() { return uniqueId; }
    public byte getUpdateType() { return updateType; }
    public byte getStateValue() { return stateValue; }
    
    // Update type constants (from RSBot)
    public static final byte UPDATE_LIFE_STATE = 0;
    public static final byte UPDATE_MOTION_STATE = 1;
    public static final byte UPDATE_BODY_STATE = 4;
    public static final byte UPDATE_PVP_STATE = 7;
    public static final byte UPDATE_BATTLE_STATE = 8;
    public static final byte UPDATE_SCROLL_STATE = 11;
    
    // Life state values
    public static final byte LIFE_ALIVE = 1;
    public static final byte LIFE_DEAD = 0;
    
    // Motion state values
    public static final byte MOTION_WALKING = 0;
    public static final byte MOTION_RUNNING = 1;
    
    public boolean isLifeStateUpdate() { return updateType == UPDATE_LIFE_STATE; }
    public boolean isMotionStateUpdate() { return updateType == UPDATE_MOTION_STATE; }
    public boolean isBodyStateUpdate() { return updateType == UPDATE_BODY_STATE; }
    public boolean isDead() { return updateType == UPDATE_LIFE_STATE && stateValue == LIFE_DEAD; }
    
    @Override
    public String toString() {
        return "LifeStateUpdateEvent{uniqueId=" + uniqueId + 
               ", updateType=" + updateType +
               ", stateValue=" + stateValue + "}";
    }
}
