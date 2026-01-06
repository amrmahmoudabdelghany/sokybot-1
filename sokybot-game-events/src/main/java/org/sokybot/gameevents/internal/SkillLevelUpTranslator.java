package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.api.events.SkillLevelUpEvent;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates skill level up packets (opcode 0x70B2) to SkillLevelUpEvent.
 * Based on TrainerHandler.skillLevelUp() pattern.
 */
@Component(service = IPacketTranslator.class)
public class SkillLevelUpTranslator implements IPacketTranslator {
    
    private static final int CHAR_SKILL_LVL_UP_OPCODE = 0x70B2;
    
    @Override
    public int getOpcode() {
        return CHAR_SKILL_LVL_UP_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean success = reader.getBoolean();
            
            if (success) {
                int skillId = reader.getInt();
                return new SkillLevelUpEvent(machineFullName, true, skillId);
            } else {
                return new SkillLevelUpEvent(machineFullName, false, 0);
            }
            
        } catch (Exception e) {
            return null;
        }
    }
}
