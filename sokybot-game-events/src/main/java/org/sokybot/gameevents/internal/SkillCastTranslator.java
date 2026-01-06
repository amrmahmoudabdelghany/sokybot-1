package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.api.events.SkillCastEvent;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates skill cast started packets (opcode 0x3844) to SkillCastEvent.
 * Based on EnvironmentHandler.onSkillCastStarted() pattern.
 */
@Component(service = IPacketTranslator.class)
public class SkillCastTranslator implements IPacketTranslator {
    
    private static final int SKILL_CAST_STARTED_OPCODE = 0x3844;
    
    @Override
    public int getOpcode() {
        return SKILL_CAST_STARTED_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean success = reader.getBoolean();
            
            if (success) {
                reader.getShort(); // Skip unknown short
                
                int skillId = reader.getInt();
                int casterId = reader.getInt();
                reader.getInt(); // Skip unknown int
                int targetId = reader.getInt();
                
                return new SkillCastEvent(machineFullName, true, skillId, casterId, targetId);
            } else {
                // Skill cast failed - return event with success=false
                return new SkillCastEvent(machineFullName, false, null, null, null);
            }
            
        } catch (Exception e) {
            return null;
        }
    }
}
