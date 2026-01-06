package org.sokybot.gameevents.internal;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.SkillLevelUpEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates skill level up packets (opcode 0x70B2) to SkillLevelUpEvent.
 * Based on TrainerHandler.skillLevelUp() pattern.
 */
public class SkillLevelUpTranslator extends AbstractTranslator {
    
    private static final int CHAR_SKILL_LVL_UP_OPCODE = 0x70B2;
    
    public SkillLevelUpTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
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
