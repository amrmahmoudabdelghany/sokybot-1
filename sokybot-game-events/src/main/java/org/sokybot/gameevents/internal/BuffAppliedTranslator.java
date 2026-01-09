package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.buff.BuffAppliedEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates buff added packets (opcode 0x30BD) to BuffAppliedEvent.
 * Based on CharacterDataReader.getBuff() pattern.
 * Uses IGameDataLookup to enrich event with buff name.
 */
public class BuffAppliedTranslator extends AbstractTranslator {
    
    private static final int BUFF_ADDED_OPCODE = 0x30BD;
    
    public BuffAppliedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return BUFF_ADDED_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int targetId = reader.getInt();  // Entity receiving the buff
            int buffRefId = reader.getInt();  // Buff/skill reference ID
            int duration = reader.getInt();   // Duration in seconds (0 = permanent)
            
            // Lookup buff name from skill data
            String buffName = null;
            if (lookup != null) {
                buffName = lookup.findSkill(buffRefId)
                    .map(skill -> skill.getName())
                    .orElse(null);
            }
            
            return singleEvent(new BuffAppliedEvent(machineFullName, buffRefId, buffName, targetId, duration));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

