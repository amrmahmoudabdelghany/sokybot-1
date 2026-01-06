package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.SkillLevelUpEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates skill level up packets (opcode 0x70B2) to SkillLevelUpEvent.
 * Based on TrainerHandler.skillLevelUp() pattern.
 * Uses IGameDataLookup to enrich event with skill name.
 */
public class SkillLevelUpTranslator extends AbstractTranslator {
    
    private static final int CHAR_SKILL_LVL_UP_OPCODE = 0xB0A1;
    
    public SkillLevelUpTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return CHAR_SKILL_LVL_UP_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean success = reader.getBoolean();
            
            if (success) {
                int skillId = reader.getInt();
                
                // Lookup skill name
                String skillName = null;
                if (lookup != null) {
                    skillName = lookup.findSkill(skillId)
                        .map(skill -> skill.getName())
                        .orElse(null);
                }
                
                return singleEvent(new SkillLevelUpEvent(machineFullName, true, skillId, skillName));
            } else {
                return singleEvent(new SkillLevelUpEvent(machineFullName, false, 0));
            }
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

