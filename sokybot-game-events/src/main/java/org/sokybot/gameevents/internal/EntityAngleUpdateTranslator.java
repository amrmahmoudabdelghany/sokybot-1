package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.EntityAngleUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates angle update packets (opcode 0x911F) to EntityAngleUpdateEvent.
 * Based on EnvironmentHandler.onAngleChanged() pattern.
 */
@Component(service = IPacketTranslator.class)
public class EntityAngleUpdateTranslator implements IPacketTranslator {
    
    private static final int ANGLE_UPDATE_OPCODE = 0x911F;
    
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
