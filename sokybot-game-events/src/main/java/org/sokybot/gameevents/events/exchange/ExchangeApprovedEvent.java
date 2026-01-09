package org.sokybot.gameevents.events.exchange;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when exchange is approved/finalized (opcode 0x3087).
 */
public class ExchangeApprovedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int exchangeId;
    private final boolean success;
    
    public ExchangeApprovedEvent(String fullName, int exchangeId, boolean success) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.exchangeId = exchangeId;
        this.success = success;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getExchangeId() { return exchangeId; }
    public boolean isSuccess() { return success; }
    
    @Override
    public String toString() {
        return String.format("ExchangeApprovedEvent[%s, exchange=%d, success=%b]", 
            fullName, exchangeId, success);
    }
}
