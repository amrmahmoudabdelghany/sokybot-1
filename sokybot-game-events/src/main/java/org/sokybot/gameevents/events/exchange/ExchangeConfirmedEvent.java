package org.sokybot.gameevents.events.exchange;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when exchange is confirmed by a party (opcode 0x3086).
 */
public class ExchangeConfirmedEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final int exchangeId;
    private final boolean selfConfirmed;  // true if self confirmed, false if partner
    
    public ExchangeConfirmedEvent(String fullName, int exchangeId, boolean selfConfirmed) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.exchangeId = exchangeId;
        this.selfConfirmed = selfConfirmed;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public int getExchangeId() { return exchangeId; }
    public boolean isSelfConfirmed() { return selfConfirmed; }
    
    @Override
    public String toString() {
        return String.format("ExchangeConfirmedEvent[%s, exchange=%d, self=%b]", 
            fullName, exchangeId, selfConfirmed);
    }
}
