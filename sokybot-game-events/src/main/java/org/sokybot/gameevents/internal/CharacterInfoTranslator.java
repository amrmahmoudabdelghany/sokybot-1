package org.sokybot.gameevents.internal;

import org.sokybot.api.events.CharacterInfoEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates character info packets (opcode 0x3011) to CharacterInfoEvent.
 * Contains combat stats: ATK/DEF/Hit/Parry/MaxHP/MaxMP.
 * Based on TrainerHandler.parsingCharInfo() pattern.
 */
public class CharacterInfoTranslator extends AbstractTranslator {
    
    private static final int CHAR_INFO_OPCODE = 0x3011;
    
    public CharacterInfoTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return CHAR_INFO_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int phyAtkMin = reader.getInt();
            int phyAtkMax = reader.getInt();
            int magAtkMin = reader.getInt();
            int magAtkMax = reader.getInt();
            
            int phyDef = reader.getShort() & 0xFFFF;
            int magDef = reader.getShort() & 0xFFFF;
            int hitRate = reader.getShort() & 0xFFFF;
            int parryRate = reader.getShort() & 0xFFFF;
            
            int maxHP = reader.getInt();
            int maxMP = reader.getInt();
            
            // Skip STR/INT shorts if not needed
            
            return new CharacterInfoEvent(machineFullName, 
                phyAtkMin, phyAtkMax, magAtkMin, magAtkMax,
                phyDef, magDef, hitRate, parryRate, maxHP, maxMP);
            
        } catch (Exception e) {
            return null;
        }
    }
}
