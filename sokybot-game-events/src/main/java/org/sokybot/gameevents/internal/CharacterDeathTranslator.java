package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.CharacterDeathEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates character death packets (opcode 0x3053) to CharacterDeathEvent.
 * Based on TrainerHandler.onCharDie() pattern.
 * Critical event for bot safety and logic.
 */
@Component(service = IPacketTranslator.class)
public class CharacterDeathTranslator implements IPacketTranslator {
    
    private static final int CHAR_DIE_OPCODE = 0x3053;
    
    @Override
    public int getOpcode() {
        return CHAR_DIE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte flag = reader.getByte();
            
            // flag == 0x04 indicates death
            if (flag == 0x04) {
                // Killer ID not available in this packet
                // Could be added from other sources if needed
                return new CharacterDeathEvent(machineFullName, null);
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
}
