package org.sokybot.gameevents.internal;

import org.sokybot.api.events.EntityAngleUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates angle update packets (opcode 0x911F) to EntityAngleUpdateEvent.
 * Based on EnvironmentHandler.onAngleChanged() pattern.
 */
public class EntityAngleUpdateTranslator extends AbstractTranslator {
    
    private static final int ANGLE_UPDATE_OPCODE = 0x911F;
    
    public EntityAngleUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return ANGLE_UPDATE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            short newAngle = reader.getShort();
            
            return new EntityAngleUpdateEvent(machineFullName, entityId, newAngle);
            
        } catch (Exception e) {
            return null;
        }
    }
}
