package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.api.events.SkillPointsUpdateEvent;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates skill points update packets (opcode 0x3842 with type SP).
 * Based on TrainerHandler.attackGainsUpdates() pattern for SP type (3).
 */
@Component(service = IPacketTranslator.class)
public class SkillPointsUpdateTranslator implements IPacketTranslator {
    
    // Uses same opcode as GoldUpdateTranslator but different gain type
    private static final int ATTACK_GAINS_UPDATE_OPCODE = 0x3843; // Different from gold
    private static final byte SP_TYPE = 3;
    
    @Override
    public int getOpcode() {
        return ATTACK_GAINS_UPDATE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte gainType = reader.getByte();
            
            // Only handle SP type (3)
            if (gainType == SP_TYPE) {
                int newSkillPoints = reader.getInt();
                return new SkillPointsUpdateEvent(machineFullName, newSkillPoints);
            }
            
            return null;
            
        } catch (Exception e) {
            return null;
        }
    }
}
