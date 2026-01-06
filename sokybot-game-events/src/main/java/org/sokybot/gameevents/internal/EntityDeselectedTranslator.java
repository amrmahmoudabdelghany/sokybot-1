package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.EntityDeselectedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates entity deselect packets (opcode 0xB04B).
 * Based on RSBot ActionDeselectResponse.
 */
public class EntityDeselectedTranslator extends AbstractTranslator {
    
    public EntityDeselectedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return 0xB04B;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            if (result != 0x01) {
                return noEvents();
            }
            
            return singleEvent(new EntityDeselectedEvent(machineFullName));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
