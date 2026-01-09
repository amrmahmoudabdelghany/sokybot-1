package org.sokybot.gameevents.events.storage;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when storage is opened (opcode 0x3047).
 * This is the begin packet for chunked storage data.
 */
public class StorageOpenEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final long storageGold;
    private final byte storageType; // 0=Personal, 1=Guild
    
    public StorageOpenEvent(String fullName, long storageGold, byte storageType) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.storageGold = storageGold;
        this.storageType = storageType;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public long getStorageGold() { return storageGold; }
    public byte getStorageType() { return storageType; }
    
    public static final byte TYPE_PERSONAL = 0;
    public static final byte TYPE_GUILD = 1;
    
    public boolean isPersonal() { return storageType == TYPE_PERSONAL; }
    public boolean isGuild() { return storageType == TYPE_GUILD; }
    
    @Override
    public String toString() {
        return "StorageOpenEvent{storageGold=" + storageGold + ", storageType=" + storageType + "}";
    }
}
