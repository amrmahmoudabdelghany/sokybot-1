package org.sokybot.api.events;

/**
 * Event fired when a quest is abandoned (opcode 0xB0D9).
 */
public class QuestAbandonEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final boolean success;
    private final int questId;
    
    public QuestAbandonEvent(String fullName, boolean success, int questId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.questId = questId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public boolean isSuccess() { return success; }
    public int getQuestId() { return questId; }
    
    @Override
    public String toString() {
        return String.format("QuestAbandonEvent[%s, success=%b, quest=%d]", fullName, success, questId);
    }
}
