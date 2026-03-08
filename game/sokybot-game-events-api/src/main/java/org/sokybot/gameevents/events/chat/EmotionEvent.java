package org.sokybot.gameevents.events.chat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when an entity performs an emotion/emote action (opcode 0x3091).
 * Emotions include sitting, waving, dancing, etc.
 */
public class EmotionEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int entityId;
    private final int emotionId;
    
    public EmotionEvent(String fullName, int entityId, int emotionId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.entityId = entityId;
        this.emotionId = emotionId;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getEntityId() { return entityId; }
    public int getEmotionId() { return emotionId; }
    
    @Override
    public String toString() {
        return String.format("EmotionEvent[%s, entity=%d, emotion=%d]", 
            fullName, entityId, emotionId);
    }
}
