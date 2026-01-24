package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.entity.EntitySpeedUpdateEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates speed update packets (opcode 0x3916) to EntitySpeedUpdateEvent.
 * Based on EnvironmentHandler.speedUpdate() pattern.
 */
public class SpeedUpdateTranslator extends AbstractTranslator {
    
    private static final int SPEED_UPDATE_OPCODE = 0x3916;
    public SpeedUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return SPEED_UPDATE_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            float walkSpeed = reader.getFloat();
            float runSpeed = reader.getFloat();
            return singleEvent(new EntitySpeedUpdateEvent(machineFullName, entityId, walkSpeed, runSpeed));
        } catch (Exception e) {
            return noEvents();
        }
}
}
