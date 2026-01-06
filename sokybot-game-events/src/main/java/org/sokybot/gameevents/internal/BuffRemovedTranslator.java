package org.sokybot.gameevents.internal;

import org.sokybot.api.events.BuffRemovedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates buff removed packets (opcode 0x30BE) to BuffRemovedEvent.
 */
public class BuffRemovedTranslator extends AbstractTranslator {
    
    private static final int BUFF_REMOVED_OPCODE = 0x30BE;
    
    public BuffRemovedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return BUFF_REMOVED_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int targetId = reader.getInt();  // Entity losing the buff
            int buffRefId = reader.getInt(); // Buff/skill reference ID
            
            return new BuffRemovedEvent(machineFullName, buffRefId);
            
        } catch (Exception e) {
            return null;
        }
    }
}
