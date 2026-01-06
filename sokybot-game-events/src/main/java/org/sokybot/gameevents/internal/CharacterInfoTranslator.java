package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.CharacterInfoEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates character info packets (opcode 0x3011) to CharacterInfoEvent.
 * Contains combat stats: ATK/DEF/Hit/Parry/MaxHP/MaxMP.
 * Based on TrainerHandler.parsingCharInfo() pattern.
 */
@Component(service = IPacketTranslator.class)
public class CharacterInfoTranslator implements IPacketTranslator {
    
    private static final int CHAR_INFO_OPCODE = 0x3011;
    
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
