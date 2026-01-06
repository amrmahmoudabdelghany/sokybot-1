package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.ExpUpdateEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates experience/SP update packets (opcode 0x38CB) to ExpUpdateEvent.
 * Based on TrainerHandler.expUpdate() pattern.
 */
@Component(service = IPacketTranslator.class)
public class ExpUpdateTranslator implements IPacketTranslator {
    
    private static final int EXP_SP_UPDATE_OPCODE = 0x38CB;
    
    @Override
    public int getOpcode() {
        return EXP_SP_UPDATE_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
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
            
            return new ExpUpdateEvent(machineFullName, expGained, currentExp, levelUp);
            
        } catch (Exception e) {
            return null;
        }
    }
}
