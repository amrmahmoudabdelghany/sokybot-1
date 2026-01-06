package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.SkillPointsUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates skill points update packets (opcode 0x3842 with type SP).
 * Based on TrainerHandler.attackGainsUpdates() pattern for SP type (3).
 */
public class SkillPointsUpdateTranslator extends AbstractTranslator {
    
    // Uses same opcode as GoldUpdateTranslator but different gain type
    private static final int ATTACK_GAINS_UPDATE_OPCODE = 0x3843; // Different from gold
    private static final byte SP_TYPE = 3;
    
    public SkillPointsUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return ATTACK_GAINS_UPDATE_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte gainType = reader.getByte();
            
            // Only handle SP type (3)
            if (gainType == SP_TYPE) {
                int newSkillPoints = reader.getInt();
                return singleEvent(new SkillPointsUpdateEvent(machineFullName, newSkillPoints));
            }
            
            return noEvents();
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
