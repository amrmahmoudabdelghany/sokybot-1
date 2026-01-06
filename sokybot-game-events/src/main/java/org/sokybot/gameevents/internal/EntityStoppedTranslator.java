package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.EntityStoppedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates spawn stuck/stopped packets (opcode 0x30B1) to EntityStoppedEvent.
 * Based on EnvironmentHandler.onStopMovement() pattern.
 */
public class EntityStoppedTranslator extends AbstractTranslator {
    
    private static final int SPAWN_STUCK_OPCODE = 0x30B1;
    
    public EntityStoppedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return SPAWN_STUCK_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            
            // Additional data available but often not needed for event
            // (position data, angle, etc. - can be added if needed)
            
            return singleEvent(new EntityStoppedEvent(machineFullName, entityId));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
