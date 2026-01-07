package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.TeleportResponseEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates teleport response packets (opcode 0xB05A) to TeleportResponseEvent.
 * Reference: SilkroadScript TeleportResponse = 0xB05A
 */
public class TeleportResponseTranslator extends AbstractTranslator {
    
    private static final int TELEPORT_RESPONSE_OPCODE = 0xB05A;
    
    public TeleportResponseTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return TELEPORT_RESPONSE_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = result == 0x01;
            
            int destinationId = 0;
            if (success) {
                destinationId = reader.getInt();
            }
            
            return singleEvent(new TeleportResponseEvent(machineFullName, success, destinationId));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
