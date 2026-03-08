package org.sokybot.gameevents.events.party;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event for party matching system events.
 * Covers party matching list, join, and member count (opcodes 0x306E, 0xB067, 0x3065).
 */
public class PartyMatchingEvent implements IGameEvent {
    
    public enum MatchingEventType {
        PLAYER_JOIN_REQUEST,  // 0x306E - Player requesting to join
        PARTY_CREATED,        // 0x3065 - Party created from matching
        MEMBER_COUNT_UPDATE   // 0xB067 - Member count changed
    }
    
    private final String fullName;
    private final long timestamp;
    private final MatchingEventType eventType;
    private final int partyId;
    private final String playerName;  // For join requests
    private final int memberCount;    // For count updates
    
    public PartyMatchingEvent(String fullName, MatchingEventType eventType, 
                              int partyId, String playerName, int memberCount) {
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
        this.eventType = eventType;
        this.partyId = partyId;
        this.playerName = playerName;
        this.memberCount = memberCount;
    }
    
    public static PartyMatchingEvent joinRequest(String fullName, int partyId, String playerName) {
        return new PartyMatchingEvent(fullName, MatchingEventType.PLAYER_JOIN_REQUEST, 
                                      partyId, playerName, 0);
    }
    
    public static PartyMatchingEvent partyCreated(String fullName, int partyId) {
        return new PartyMatchingEvent(fullName, MatchingEventType.PARTY_CREATED, 
                                      partyId, null, 0);
    }
    
    public static PartyMatchingEvent memberCountUpdate(String fullName, int partyId, int count) {
        return new PartyMatchingEvent(fullName, MatchingEventType.MEMBER_COUNT_UPDATE, 
                                      partyId, null, count);
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public MatchingEventType getEventType() { return eventType; }
    public int getPartyId() { return partyId; }
    public String getPlayerName() { return playerName; }
    public int getMemberCount() { return memberCount; }
    
    @Override
    public String toString() {
        return String.format("PartyMatchingEvent[%s, type=%s, party=%d]", 
            fullName, eventType, partyId);
    }
}
