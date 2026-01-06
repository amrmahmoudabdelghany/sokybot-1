package org.sokybot.api.events;

/**
 * Event emitted when quest state changes (opcode 0x30D5).
 * Update types: Add, Remove, Abandon, Update.
 */
public class QuestUpdateEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte updateType;
    private final int questId;
    
    public QuestUpdateEvent(String fullName, byte updateType, int questId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.updateType = updateType;
        this.questId = questId;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getUpdateType() { return updateType; }
    public int getQuestId() { return questId; }
    
    // Update type constants (from RSBot QuestUpdateType enum)
    public static final byte TYPE_ABANDON = 0x01;
    public static final byte TYPE_REMOVE = 0x02;
    public static final byte TYPE_ADD = 0x03;
    public static final byte TYPE_UPDATE = 0x04;
    
    public boolean isAdded() { return updateType == TYPE_ADD; }
    public boolean isRemoved() { return updateType == TYPE_REMOVE; }
    public boolean isAbandoned() { return updateType == TYPE_ABANDON; }
    public boolean isUpdated() { return updateType == TYPE_UPDATE; }
    
    @Override
    public String toString() {
        return "QuestUpdateEvent{updateType=" + updateType + 
               ", questId=" + questId + "}";
    }
}
