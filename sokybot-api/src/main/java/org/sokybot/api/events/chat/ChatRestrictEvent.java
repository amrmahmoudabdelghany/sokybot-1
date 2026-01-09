package org.sokybot.api.events;

/**
 * Event fired for chat restriction (opcode 0x302D).
 * Player is temporarily restricted from chatting.
 */
public class ChatRestrictEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int durationSeconds;  // Restriction duration
    
    public ChatRestrictEvent(String fullName, int durationSeconds) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.durationSeconds = durationSeconds;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getDurationSeconds() { return durationSeconds; }
    
    @Override
    public String toString() {
        return String.format("ChatRestrictEvent[%s, duration=%ds]", fullName, durationSeconds);
    }
}
