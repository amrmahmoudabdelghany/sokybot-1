package org.sokybot.api.events;

/**
 * Event emitted when a party invite or request is received (opcode 0x3080).
 */
public class PartyInviteEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    
    private final byte requestType; // 0=Party1, 1=Party2, etc. (Simulated enum)
    // Additional data like inviter ID could be added if parsed
    
    public PartyInviteEvent(String fullName, byte requestType) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.requestType = requestType;
    }
    
    @Override
    public String getFullName() {
        return fullName;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    public byte getRequestType() { return requestType; }
    
    @Override
    public String toString() {
        return "PartyInviteEvent{requestType=" + requestType + "}";
    }
}
