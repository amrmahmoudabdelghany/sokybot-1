package org.sokybot.gameevents.internal;

import org.sokybot.api.events.CharacterDeathEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates character death packets (opcode 0x3053) to CharacterDeathEvent.
 * Based on TrainerHandler.onCharDie() pattern.
 * Critical event for bot safety and logic.
 */
public class CharacterDeathTranslator extends AbstractTranslator {
    
    private static final int CHAR_DIE_OPCODE = 0x3053;
    
    public CharacterDeathTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
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
