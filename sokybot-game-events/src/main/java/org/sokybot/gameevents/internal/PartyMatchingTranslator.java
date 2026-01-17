package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.party.PartyMatchingEvent;
import org.sokybot.gameevents.events.party.PartyMatchingEvent.MatchingEventType;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates party matching system event packets.
 * Handles: 0x306E (player join request), 0xB067 (member count), 0x3065 (party created).
 * Reference: go-sro-framework Party matching opcodes
 */
public class PartyMatchingTranslator extends AbstractTranslator {
    
    private final int opcode;
    private final MatchingEventType eventType;
    public PartyMatchingTranslator(IGameDataLookup lookup, int opcode, MatchingEventType eventType) {
        super(lookup);
        this.opcode = opcode;
        this.eventType = eventType;
    }
    public static PartyMatchingTranslator forPlayerJoinRequest(IGameDataLookup lookup) {
        return new PartyMatchingTranslator(lookup, 0x306E, MatchingEventType.PLAYER_JOIN_REQUEST);
    public static PartyMatchingTranslator forPartyCreated(IGameDataLookup lookup) {
        return new PartyMatchingTranslator(lookup, 0x3065, MatchingEventType.PARTY_CREATED);
    public static PartyMatchingTranslator forMemberCountUpdate(IGameDataLookup lookup) {
        return new PartyMatchingTranslator(lookup, 0xB067, MatchingEventType.MEMBER_COUNT_UPDATE);
    @Override
    public int getOpcode() {
        return opcode;
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            PartyMatchingEvent event;
            switch (eventType) {
                case PLAYER_JOIN_REQUEST:
                    int partyIdJoin = reader.getInt();
                    int nameLen = reader.getShort() & 0xFFFF;
                    String playerName = reader.getUnicodeString(nameLen);
                    event = PartyMatchingEvent.joinRequest(machineFullName, partyIdJoin, playerName);
                    break;
                    
                case PARTY_CREATED:
                    int partyIdCreated = reader.getInt();
                    event = PartyMatchingEvent.partyCreated(machineFullName, partyIdCreated);
                case MEMBER_COUNT_UPDATE:
                    int partyIdCount = reader.getInt();
                    int memberCount = reader.getByte() & 0xFF;
                    event = PartyMatchingEvent.memberCountUpdate(machineFullName, partyIdCount, memberCount);
                default:
                    return noEvents();
            }
            return singleEvent(event);
        } catch (Exception e) {
            return noEvents();
        }
}
