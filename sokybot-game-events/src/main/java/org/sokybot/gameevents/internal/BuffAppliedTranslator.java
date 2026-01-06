package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.BuffAppliedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates buff added packets (opcode 0x30BD) to BuffAppliedEvent.
 * Based on CharacterDataReader.getBuff() pattern.
 */
@Component(service = IPacketTranslator.class)
public class BuffAppliedTranslator implements IPacketTranslator {
    
    private static final int BUFF_ADDED_OPCODE = 0x30BD;
    
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
