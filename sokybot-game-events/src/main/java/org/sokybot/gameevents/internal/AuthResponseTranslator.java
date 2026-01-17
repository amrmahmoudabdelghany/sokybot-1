package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.session.AuthResponseEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates Auth response packets (opcode 0xA103).
 */
public class AuthResponseTranslator extends AbstractTranslator {
    
    private static final int AUTH_RESPONSE_OPCODE = 0xA103;
    public AuthResponseTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return AUTH_RESPONSE_OPCODE;
    }
    
    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte resultCode = reader.getByte();
            boolean success = (resultCode == 0x01); // Assuming 1 is success, 2 is fail (from controller code)
            // Controller: if (result == 0x02) failed. else success.
            // So resultCode != 0x02 is success. 
            // Standard SRO: 1=success usually.
            // Let's stick with controller logic: if result == 0x02 fail.
            if (resultCode == 0x02) {
                success = false;
            } else {
                success = true;
            }
            return singleEvent(new AuthResponseEvent(machineFullName, success, resultCode));
        } catch (Exception e) {
            return noEvents();
        }
    }
}
