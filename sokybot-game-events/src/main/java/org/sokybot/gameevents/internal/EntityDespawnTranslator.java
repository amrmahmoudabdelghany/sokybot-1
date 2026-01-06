package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.EntityDespawnEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates entity despawn packets (opcode 0x3017) to EntityDespawnEvent.
 */
@Component(service = IPacketTranslator.class)
public class EntityDespawnTranslator implements IPacketTranslator {
    
    private static final int ENTITY_DESPAWN_OPCODE = 0x3017;
    
    @Override
    public int getOpcode() {
        return ENTITY_DESPAWN_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        // Based on engine's EnvironmentHandler.onDespawn() pattern
        
        try {
            var reader = packet.getStreamReader();
            
            // Despawn packet only contains the uniqueId of the entity to remove
            int entityId = reader.getInt();
            
            return new EntityDespawnEvent(machineFullName, entityId);
            
        } catch (Exception e) {
            return null;
        }
    }
}
