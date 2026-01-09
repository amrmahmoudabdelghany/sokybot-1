package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.skill.SkillCastEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates skill cast started packets (opcode 0x3844) to SkillCastEvent.
 * Based on EnvironmentHandler.onSkillCastStarted() pattern.
 * Uses IGameDataLookup to enrich event with skill name.
 */
public class SkillCastTranslator extends AbstractTranslator {
    
    private static final int SKILL_CAST_STARTED_OPCODE = 0x3844;
    
    public SkillCastTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return SKILL_CAST_STARTED_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            boolean success = reader.getBoolean();
            
            if (success) {
                reader.getShort(); // Skip unknown short
                
                int skillId = reader.getInt();
                int casterId = reader.getInt();
                reader.getInt(); // Skip unknown int
                int targetId = reader.getInt();
                
                // Lookup skill name from skill data
                String skillName = null;
                if (lookup != null) {
                    skillName = lookup.findSkill(skillId)
                        .map(skill -> skill.getName())
                        .orElse(null);
                }
                
                return singleEvent(new SkillCastEvent(machineFullName, true, skillId, skillName, casterId, targetId));
            } else {
                // Skill cast failed - return event with success=false
                return singleEvent(new SkillCastEvent(machineFullName, false, null, null, null));
            }
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

