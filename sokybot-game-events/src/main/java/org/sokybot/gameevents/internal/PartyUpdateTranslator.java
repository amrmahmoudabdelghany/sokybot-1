package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.party.PartyUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates party update packets (opcode 0x3864).
 * Handles: Dismissed, Joined, Leave, Member updates, Leader changes.
 * Based on RSBot PartyUpdateResponse.
 */
public class PartyUpdateTranslator extends AbstractTranslator {
    
    public PartyUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x3864;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte updateType = reader.getByte();
            Integer memberUniqueId = null;
            String memberName = null;
            Byte memberLevel = null;
            Byte memberHealthMana = null;
            switch (updateType) {
                case PartyUpdateEvent.TYPE_DISMISSED:
                    // No additional data
                    break;
                    
                case PartyUpdateEvent.TYPE_JOINED:
                    // Parse member info
                    memberUniqueId = reader.getInt();
                    memberName = reader.getString();
                    memberLevel = reader.getByte();
                    // More member data follows but skip for now
                case PartyUpdateEvent.TYPE_LEAVE:
                case PartyUpdateEvent.TYPE_MEMBER:
                    byte memberUpdateType = reader.getByte();
                    // Parse based on member update type
                    switch (memberUpdateType) {
                        case 0x04: // HPMP
                            memberHealthMana = reader.getByte();
                            break;
                        case 0x08: // Level
                            memberLevel = reader.getByte();
                        default:
                            // Other update types
                    }
                case PartyUpdateEvent.TYPE_LEADER:
                case PartyUpdateEvent.TYPE_LEADER_CHANGE:
                default:
            }
            return singleEvent(new PartyUpdateEvent(
                machineFullName, updateType, memberUniqueId, memberName, memberLevel, memberHealthMana));
        } catch (Exception e) {
            return noEvents();
        }
}
}
