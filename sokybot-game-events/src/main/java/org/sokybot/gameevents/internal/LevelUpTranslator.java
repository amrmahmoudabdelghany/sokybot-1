package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.LevelUpEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates level up/promotion animation packets (opcode 0x3054) to LevelUpEvent.
 * Reference: RSBot EntityAnimationPromoteResponse = 0x3054
 */
public class LevelUpTranslator extends AbstractTranslator {
    
    private static final int LEVEL_UP_OPCODE = 0x3054;
    
    public LevelUpTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return LEVEL_UP_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int entityId = reader.getInt();
            
            return singleEvent(new LevelUpEvent(machineFullName, entityId));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
