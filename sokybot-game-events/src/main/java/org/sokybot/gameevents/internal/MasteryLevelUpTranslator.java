package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.skill.MasteryLevelUpEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates mastery level up packets (opcode 0xB0A2) to MasteryLevelUpEvent.
 * Based on TrainerHandler.masteryLevelUp() pattern.
 * Uses IGameDataLookup to enrich event with mastery name.
 */
public class MasteryLevelUpTranslator extends AbstractTranslator {
    
    private static final int CHAR_MASTERY_LVL_UP_OPCODE = 0xB0A2;
    public MasteryLevelUpTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return CHAR_MASTERY_LVL_UP_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = (result == 1);
            if (success) {
                int masteryId = reader.getInt();
                int newLevel = reader.getByte() & 0xFF;
                
                // Lookup mastery name
                String masteryName = null;
                if (lookup != null) {
                    masteryName = lookup.findMasteryName(masteryId).orElse(null);
                }
                return singleEvent(new MasteryLevelUpEvent(machineFullName, true, masteryId, masteryName, newLevel));
            } else {
                return singleEvent(new MasteryLevelUpEvent(machineFullName, false, 0, 0));
            }
        } catch (Exception e) {
            return noEvents();
        }
}
}
