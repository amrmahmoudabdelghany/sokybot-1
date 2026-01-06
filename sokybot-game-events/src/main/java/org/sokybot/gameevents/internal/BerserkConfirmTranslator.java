package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.BerserkConfirmEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates berserk confirm packets (opcode 0xB0A7) to BerserkConfirmEvent.
 * Based on ServerOpcode.BESERK_CONFIRM definition.
 */
@Component(service = IPacketTranslator.class)
public class BerserkConfirmTranslator implements IPacketTranslator {
    
    private static final int BESERK_CONFIRM_OPCODE = 0xB0A7;
    
    @Override
    public int getOpcode() {
        return BESERK_CONFIRM_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean success = reader.getBoolean();
            byte berserkLevel = 0;
            
            if (success) {
                berserkLevel = reader.getByte();
            }
            
            return new BerserkConfirmEvent(machineFullName, success, berserkLevel);
            
        } catch (Exception e) {
            return null;
        }
    }
}
