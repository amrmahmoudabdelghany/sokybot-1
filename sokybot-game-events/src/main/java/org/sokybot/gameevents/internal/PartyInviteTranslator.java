package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.party.PartyInviteEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates party invite packets (opcode 0x3080).
 * Based on RSBot PartyInviteResponse.
 */
public class PartyInviteTranslator extends AbstractTranslator {
    
    public PartyInviteTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x3080;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte requestType = reader.getByte();
            // Additional parsing can be added here if needed (e.g., inviter info)
            return singleEvent(new PartyInviteEvent(machineFullName, requestType));
        } catch (Exception e) {
            return noEvents();
        }
}
}
