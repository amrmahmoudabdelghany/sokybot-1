package org.sokybot.api.events;

/**
 * Event emitted when a player exchange/trade is cancelled (opcode 0x3088).
 */
public class ExchangeCancelledEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    public ExchangeCancelledEvent(String fullName) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "ExchangeCancelledEvent{}";
    }
}
