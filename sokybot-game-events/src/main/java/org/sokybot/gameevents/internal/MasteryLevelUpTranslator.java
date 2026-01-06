package org.sokybot.gameevents.internal;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.MasteryLevelUpEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates mastery level up packets (opcode 0x70B5) to MasteryLevelUpEvent.
 * Based on TrainerHandler.masteryLevelUp() pattern.
 */
public class MasteryLevelUpTranslator extends AbstractTranslator {
    
    private static final int CHAR_MASTERY_LVL_UP_OPCODE = 0x70B5;
    
    public MasteryLevelUpTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return CHAR_MASTERY_LVL_UP_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = (result == 1);
            
            if (success) {
                int masteryId = reader.getInt();
                int newLevel = reader.getByte() & 0xFF;
                
                return new MasteryLevelUpEvent(machineFullName, true, masteryId, newLevel);
            } else {
                return new MasteryLevelUpEvent(machineFullName, false, 0, 0);
            }
            
        } catch (Exception e) {
            return null;
        }
    }
}
