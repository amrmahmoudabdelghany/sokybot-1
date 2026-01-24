package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.character.CharacterDeathEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates character death packets (opcode 0x3053) to CharacterDeathEvent.
 * Based on TrainerHandler.onCharDie() pattern.
 * Critical event for bot safety and logic.
 */
public class CharacterDeathTranslator extends AbstractTranslator {
    
    private static final int CHAR_DIE_OPCODE = 0x3053;
    public CharacterDeathTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return CHAR_DIE_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte flag = reader.getByte();
            // flag == 0x04 indicates death
            if (flag == 0x04) {
                // Killer ID not available in this packet
                // Could be added from other sources if needed
                return singleEvent(new CharacterDeathEvent(machineFullName, null));
            }
            return noEvents();
        } catch (Exception e) {
            return noEvents();
        }
}
}
