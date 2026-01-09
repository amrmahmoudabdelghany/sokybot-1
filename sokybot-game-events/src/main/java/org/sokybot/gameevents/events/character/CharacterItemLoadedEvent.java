package org.sokybot.gameevents.events.character;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Fine-grained event emitted for each item detected during character data loading.
 * Provides full item details with name lookup from game data.
 */
public class CharacterItemLoadedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte slot;
    private final int itemId;
    private final String itemName;
    private final int stackCount;
    private final int optLevel;      // Enhancement level (+1, +2, etc.)
    private final boolean isEquipped;
    
    public CharacterItemLoadedEvent(String fullName, byte slot, int itemId, 
                                    String itemName, int stackCount, int optLevel, boolean isEquipped) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.slot = slot;
        this.itemId = itemId;
        this.itemName = itemName;
        this.stackCount = stackCount;
        this.optLevel = optLevel;
        this.isEquipped = isEquipped;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getSlot() { return slot; }
    public int getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public int getStackCount() { return stackCount; }
    public int getOptLevel() { return optLevel; }
    public boolean isEquipped() { return isEquipped; }
    
    @Override
    public String toString() {
        return "CharacterItemLoadedEvent{slot=" + slot + 
               ", itemId=" + itemId + 
               ", itemName='" + itemName + "'" +
               ", stackCount=" + stackCount +
               ", optLevel=+" + optLevel +
               ", isEquipped=" + isEquipped + "}";
    }
}

