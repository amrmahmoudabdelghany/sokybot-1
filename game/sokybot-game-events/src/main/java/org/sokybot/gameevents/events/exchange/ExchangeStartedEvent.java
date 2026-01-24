package org.sokybot.gameevents.events.exchange;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event emitted when a player exchange/trade is started (opcode 0x3085).
 */
public class ExchangeStartedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final int otherPlayerUniqueId;
    
    public ExchangeStartedEvent(String fullName, int otherPlayerUniqueId) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.otherPlayerUniqueId = otherPlayerUniqueId;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getOtherPlayerUniqueId() { return otherPlayerUniqueId; }
    
    @Override
    public String toString() {
        return "ExchangeStartedEvent{otherPlayerUniqueId=" + otherPlayerUniqueId + "}";
    }
}
