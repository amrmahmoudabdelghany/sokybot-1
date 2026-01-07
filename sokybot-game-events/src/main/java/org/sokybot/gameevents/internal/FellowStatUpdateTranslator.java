package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.FellowStatUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates Fellow stat update packets (opcode 0x3422).
 * This is fired when a Fellow pet's combat stats change.
 * Based on RSBot FellowStatUpdateResponse.
 */
public class FellowStatUpdateTranslator extends AbstractTranslator {
    
    public FellowStatUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return 0x3422;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            int uniqueId = reader.getInt();
            
            int strength = reader.getShort() & 0xFFFF;
            int intelligence = reader.getShort() & 0xFFFF;
            
            int physicalAttackMin = reader.getInt();
            int physicalAttackMax = reader.getInt();
            int magicalAttackMin = reader.getInt();
            int magicalAttackMax = reader.getInt();
            
            int physicalDefence = reader.getShort() & 0xFFFF;
            int magicalDefence = reader.getShort() & 0xFFFF;
            reader.getShort(); // phy absorb
            reader.getShort(); // mag absorb
            
            int hitRate = reader.getShort() & 0xFFFF;
            reader.getShort(); // parry rate
            reader.getShort(); // critical
            reader.getShort(); // blocking rate
            
            int maxHealth = reader.getInt();
            
            return singleEvent(new FellowStatUpdateEvent(
                machineFullName, uniqueId,
                strength, intelligence,
                physicalAttackMin, physicalAttackMax,
                magicalAttackMin, magicalAttackMax,
                physicalDefence, magicalDefence,
                hitRate, maxHealth));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
