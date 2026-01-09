package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.skill.SkillCastEndEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates skill cast ended packets (opcode 0xB071) to SkillCastEndEvent.
 * Based on ServerOpcode.SKILL_CAST_ENDED definition.
 * Fires when a skill finishes casting or is interrupted.
 * Uses IGameDataLookup to enrich event with skill name.
 */
public class SkillCastEndTranslator extends AbstractTranslator {
    
    private static final int SKILL_CAST_ENDED_OPCODE = 0xB071;
    
    public SkillCastEndTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return SKILL_CAST_ENDED_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int casterId = reader.getInt();
            int skillId = reader.getInt();
            
            // Lookup skill name
            String skillName = null;
            if (lookup != null) {
                skillName = lookup.findSkill(skillId)
                    .map(skill -> skill.getName())
                    .orElse(null);
            }
            
            return singleEvent(new SkillCastEndEvent(machineFullName, casterId, skillId, skillName));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

