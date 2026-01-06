package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.api.events.SkillCastConfirmEvent;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates skill cast confirm packets (opcode 0xB074) to SkillCastConfirmEvent.
 * Based on ServerOpcode.SKILL_CAST_CONFIRM - action confirmation event.
 * Indicates if skill was added to queue and queue position.
 */
@Component(service = IPacketTranslator.class)
public class SkillCastConfirmTranslator implements IPacketTranslator {
    
    private static final int SKILL_CAST_CONFIRM_OPCODE = 0xB074;
    
    @Override
    public int getOpcode() {
        return SKILL_CAST_CONFIRM_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Based on TrainerHandler.skillCastConfirm() comments:
            // 1-byte OK / added to queue
            // 1-byte Position in Queue
            boolean success = reader.getBoolean();
            byte queuePosition = reader.getByte();
            
            return new SkillCastConfirmEvent(machineFullName, success, queuePosition);
            
        } catch (Exception e) {
            return null;
        }
    }
}
