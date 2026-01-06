package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.api.events.SkillCastEndEvent;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates skill cast ended packets (opcode 0xB071) to SkillCastEndEvent.
 * Based on ServerOpcode.SKILL_CAST_ENDED definition.
 * Fires when a skill finishes casting or is interrupted.
 */
@Component(service = IPacketTranslator.class)
public class SkillCastEndTranslator implements IPacketTranslator {
    
    private static final int SKILL_CAST_ENDED_OPCODE = 0xB071;
    
    @Override
    public int getOpcode() {
        return SKILL_CAST_ENDED_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int casterId = reader.getInt();
            int skillId = reader.getInt();
            
            return new SkillCastEndEvent(machineFullName, casterId, skillId);
            
        } catch (Exception e) {
            return null;
        }
    }
}
