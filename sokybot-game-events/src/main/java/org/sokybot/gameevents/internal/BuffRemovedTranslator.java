package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.BuffRemovedEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates buff removed packets (opcode 0x30BE) to BuffRemovedEvent.
 * Uses IGameDataLookup to enrich event with buff name.
 */
public class BuffRemovedTranslator extends AbstractTranslator {
    
    private static final int BUFF_REMOVED_OPCODE = 0x30BE;
    
    public BuffRemovedTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return BUFF_REMOVED_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int targetId = reader.getInt();  // Entity losing the buff
            int buffRefId = reader.getInt(); // Buff/skill reference ID
            
            // Lookup buff name from skill data
            String buffName = null;
            if (lookup != null) {
                buffName = lookup.findSkill(buffRefId)
                    .map(skill -> skill.getName())
                    .orElse(null);
            }
            
            return singleEvent(new BuffRemovedEvent(machineFullName, buffRefId, buffName));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}

