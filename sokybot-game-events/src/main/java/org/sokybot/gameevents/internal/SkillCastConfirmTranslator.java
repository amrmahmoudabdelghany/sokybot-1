package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.skill.SkillCastConfirmEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates skill cast confirm packets (opcode 0xB074) to SkillCastConfirmEvent.
 * Based on ServerOpcode.SKILL_CAST_CONFIRM - action confirmation event.
 * Indicates if skill was added to queue and queue position.
 */
public class SkillCastConfirmTranslator extends AbstractTranslator {
    
    private static final int SKILL_CAST_CONFIRM_OPCODE = 0xB074;
    
    public SkillCastConfirmTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return SKILL_CAST_CONFIRM_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Based on TrainerHandler.skillCastConfirm() comments:
            // 1-byte OK / added to queue
            // 1-byte Position in Queue
            boolean success = reader.getBoolean();
            byte queuePosition = reader.getByte();
            
            return singleEvent(new SkillCastConfirmEvent(machineFullName, success, queuePosition));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
