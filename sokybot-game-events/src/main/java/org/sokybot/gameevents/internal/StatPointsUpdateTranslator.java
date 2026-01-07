package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.StatPointsUpdateEvent;
import org.sokybot.api.events.StatPointsUpdateEvent.StatType;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates stat point update packets (opcodes 0xB050/0xB051) to StatPointsUpdateEvent.
 * Handles both STR and INT stat point allocation responses.
 * Reference: SilkroadScript StrUp = 0xB050, IntUp = 0xB051
 */
public class StatPointsUpdateTranslator extends AbstractTranslator {
    
    private final int opcode;
    private final StatType statType;
    
    public StatPointsUpdateTranslator(IGameDataLookup lookup, int opcode, StatType statType) {
        super(lookup);
        this.opcode = opcode;
        this.statType = statType;
    }
    
    /**
     * Factory method for STR update translator (0xB050)
     */
    public static StatPointsUpdateTranslator forStrength(IGameDataLookup lookup) {
        return new StatPointsUpdateTranslator(lookup, 0xB050, StatType.STRENGTH);
    }
    
    /**
     * Factory method for INT update translator (0xB051)
     */
    public static StatPointsUpdateTranslator forIntelligence(IGameDataLookup lookup) {
        return new StatPointsUpdateTranslator(lookup, 0xB051, StatType.INTELLIGENCE);
    }
    
    @Override
    public int getOpcode() {
        return opcode;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            // Read success flag
            byte result = reader.getByte();
            boolean success = result == 0x01;
            
            int remainingPoints = 0;
            if (success) {
                // Read remaining stat points after allocation
                remainingPoints = reader.getShort() & 0xFFFF;
            }
            
            return singleEvent(new StatPointsUpdateEvent(machineFullName, statType, success, remainingPoints));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
