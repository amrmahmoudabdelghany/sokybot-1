package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.EntityStoppedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates spawn stuck/stopped packets (opcode 0x30B1) to EntityStoppedEvent.
 * Based on EnvironmentHandler.onStopMovement() pattern.
 */
@Component(service = IPacketTranslator.class)
public class EntityStoppedTranslator implements IPacketTranslator {
    
    private static final int SPAWN_STUCK_OPCODE = 0x30B1;
    
    @Override
    public int getOpcode() {
        return SPAWN_STUCK_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            
            // Additional data available but often not needed for event
            // (position data, angle, etc. - can be added if needed)
            
            return new EntityStoppedEvent(machineFullName, entityId);
            
        } catch (Exception e) {
            return null;
        }
    }
}
