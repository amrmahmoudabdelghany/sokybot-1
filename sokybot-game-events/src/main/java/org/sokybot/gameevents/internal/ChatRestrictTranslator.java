package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.chat.ChatRestrictEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates chat restriction packets (opcode 0x302D) to ChatRestrictEvent.
 * Reference: go-sro-framework ChatRestrict = 0x302D
 */
public class ChatRestrictTranslator extends AbstractTranslator {
    
    private static final int CHAT_RESTRICT_OPCODE = 0x302D;
    
    public ChatRestrictTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return CHAT_RESTRICT_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            int durationSeconds = reader.getInt();
            
            return singleEvent(new ChatRestrictEvent(machineFullName, durationSeconds));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
