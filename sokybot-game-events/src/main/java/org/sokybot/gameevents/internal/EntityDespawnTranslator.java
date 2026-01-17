package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates entity despawn packets (opcode 0x3017) to EntityDespawnEvent.
 */
public class EntityDespawnTranslator extends AbstractTranslator {
    
    private static final int ENTITY_DESPAWN_OPCODE = 0x3017;
    public EntityDespawnTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return ENTITY_DESPAWN_OPCODE;
    }
    
    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        // Based on engine's EnvironmentHandler.onDespawn() pattern
        
        try {
            var reader = packet.getStreamReader();
            
            // Despawn packet only contains the uniqueId of the entity to remove
            int entityId = reader.getInt();
            return singleEvent(new EntityDespawnEvent(machineFullName, entityId));
        } catch (Exception e) {
            return noEvents();
        }
    }
}
