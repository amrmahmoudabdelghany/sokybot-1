package org.sokybot.gameevents.events.alchemy;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when item magic options are updated (opcode 0x34AA).
 * Used for alchemy enhancements and modifications.
 */
public class MagicOptionUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final byte slot;
    private final int errorCode;  // Only if failed
    
    public MagicOptionUpdateEvent(String fullName, boolean success, byte slot, int errorCode) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.slot = slot;
        this.errorCode = errorCode;
    }
    
    public static MagicOptionUpdateEvent success(String fullName, byte slot) {
        return new MagicOptionUpdateEvent(fullName, true, slot, 0);
    }
    
    public static MagicOptionUpdateEvent failure(String fullName, int errorCode) {
        return new MagicOptionUpdateEvent(fullName, false, (byte) 0, errorCode);
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public byte getSlot() { return slot; }
    public int getErrorCode() { return errorCode; }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("MagicOptionUpdateEvent[%s, success, slot=%d]", fullName, slot);
        } else {
            return String.format("MagicOptionUpdateEvent[%s, failed, error=%d]", fullName, errorCode);
        }
    }
}
