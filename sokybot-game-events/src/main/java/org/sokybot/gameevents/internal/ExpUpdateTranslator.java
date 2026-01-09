package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.stat.ExpUpdateEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates experience/SP update packets (opcode 0x3056) to ExpUpdateEvent.
 * Based on TrainerHandler.expUpdate() pattern.
 */
public class ExpUpdateTranslator extends AbstractTranslator {
    
    private static final int EXP_SP_UPDATE_OPCODE = 0x3056;
    
    public ExpUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return EXP_SP_UPDATE_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int monsterId = reader.getInt(); // Not used in event currently
            
            // EXP calculation: current - previous
            int currentExp = reader.getInt();
            int previousExp = reader.getInt();
            long expGained = currentExp - previousExp;
            
            // Skip SP values (not used for this event)
            reader.getInt(); // current SP
            reader.getInt(); // previous SP
            
            // Simplified - actual level up detection would need more context
            // The engine does this by comparing against max exp for level
            boolean levelUp = false; // TODO: Could be enhanced with level tracking
            
            return singleEvent(new ExpUpdateEvent(machineFullName, expGained, currentExp, levelUp));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
