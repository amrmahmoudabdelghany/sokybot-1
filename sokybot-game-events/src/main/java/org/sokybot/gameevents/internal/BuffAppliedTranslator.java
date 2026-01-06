package org.sokybot.gameevents.internal;

import org.sokybot.api.events.BuffAppliedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates buff added packets (opcode 0x30BD) to BuffAppliedEvent.
 * Based on CharacterDataReader.getBuff() pattern.
 */
public class BuffAppliedTranslator extends AbstractTranslator {
    
    private static final int BUFF_ADDED_OPCODE = 0x30BD;
    
    public BuffAppliedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return BUFF_ADDED_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int targetId = reader.getInt();  // Entity receiving the buff
            int buffRefId = reader.getInt();  // Buff/skill reference ID
            int duration = reader.getInt();   // Duration in seconds (0 = permanent)
            
            // Note: CharacterDataReader also handles transferableBuff flag
            // but that requires skill entity lookup - skip for basic event
            
            return new BuffAppliedEvent(machineFullName, buffRefId, targetId, duration);
            
        } catch (Exception e) {
            return null;
        }
    }
}
