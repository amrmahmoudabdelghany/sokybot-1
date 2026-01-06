package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.BuffRemovedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates buff removed packets (opcode 0x30BE) to BuffRemovedEvent.
 */
@Component(service = IPacketTranslator.class)
public class BuffRemovedTranslator implements IPacketTranslator {
    
    private static final int BUFF_REMOVED_OPCODE = 0x30BE;
    
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
