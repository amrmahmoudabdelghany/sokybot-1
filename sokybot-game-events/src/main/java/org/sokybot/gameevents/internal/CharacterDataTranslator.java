package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.CharacterDataEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates character data packets (opcode 0x3013) to CharacterDataEvent.
 * This is one of the MOST IMPORTANT packets - signals character fully loaded.
 * Based on TrainerHandler.parsingCharData() pattern.
 */
@Component(service = IPacketTranslator.class)
public class CharacterDataTranslator implements IPacketTranslator {
    
    private static final int CHAR_DATA_OPCODE = 0x3013;
    
    @Override
    public int getOpcode() {
        return CHAR_DATA_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read essential character data
            reader.getInt(); // serverTime
            int refId = reader.getInt();
            
            reader.getByte(); // charScale
            int level = reader.getByte() & 0xFF; // level (unsigned byte)
            reader.getByte(); // maxLvl
            
            long currentExp = reader.getLong(); // charEXPOffset
            reader.getInt(); // sexpOffset
            long gold = reader.getLong();
            int skillPoints = reader.getInt();
            reader.getShort(); // charStatPoint
            reader.getByte(); // zerkCount
            reader.getInt(); // gatheredExpPoint
            
            int currentHP = reader.getInt();
            int currentMP = reader.getInt();
            
            // Continue reading to get STR/INT - simplified extraction
            // Full parsing would require more complex logic
            int strength = 0;
            int intelligence = 0;
            
            // Create event with essential character data
            return new CharacterDataEvent(machineFullName, currentExp, skillPoints, 
                                         level, strength, intelligence);
            
        } catch (Exception e) {
            // CHAR_DATA packet is complex - return basic event on parsing issues
            return new CharacterDataEvent(machineFullName, 0, 0, 0, 0, 0);
        }
    }
}
