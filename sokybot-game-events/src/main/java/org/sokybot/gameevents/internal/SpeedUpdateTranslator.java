package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.EntitySpeedUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates speed update packets (opcode 0x3916) to EntitySpeedUpdateEvent.
 * Based on EnvironmentHandler.speedUpdate() pattern.
 */
@Component(service = IPacketTranslator.class)
public class SpeedUpdateTranslator implements IPacketTranslator {
    
    private static final int SPEED_UPDATE_OPCODE = 0x3916;
    
    @Override
    public int getOpcode() {
        return SPEED_UPDATE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            float walkSpeed = reader.getFloat();
            float runSpeed = reader.getFloat();
            
            return new EntitySpeedUpdateEvent(machineFullName, entityId, walkSpeed, runSpeed);
            
        } catch (Exception e) {
            return null;
        }
    }
}
