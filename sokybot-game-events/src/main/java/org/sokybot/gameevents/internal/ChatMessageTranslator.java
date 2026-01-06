package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.ChatMessageEvent;
import org.sokybot.api.events.ChatMessageEvent.ChatType;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates chat update packets (opcode 0x3026) to ChatMessageEvent.
 * Based on ServerOpcode.CHAT_UPDATE definition.
 */
public class ChatMessageTranslator extends AbstractTranslator {
    
    private static final int CHAT_UPDATE_OPCODE = 0x3026;
    
    public ChatMessageTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return CHAT_UPDATE_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte chatTypeValue = reader.getByte();
            ChatType chatType = mapChatType(chatTypeValue);
            
            // Read sender name length and name
            int senderNameLength = reader.getShort() & 0xFFFF;
            String senderName = new String(reader.getBytes(senderNameLength));
            
            // Read message length and message
            int messageLength = reader.getShort() & 0xFFFF;
            String message = new String(reader.getBytes(messageLength));
            
            return singleEvent(new ChatMessageEvent(machineFullName, chatType, senderName, message));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
    
    private ChatType mapChatType(byte value) {
        switch (value) {
            case 1: return ChatType.ALL;
            case 2: return ChatType.PRIVATE;
            case 3: return ChatType.PARTY;
            case 4: return ChatType.GUILD;
            case 5: return ChatType.GLOBAL;
            case 6: return ChatType.NOTICE;
            case 7: return ChatType.STALL;
            case 9: return ChatType.UNION;
            case 11: return ChatType.ACADEMY;
            default: return ChatType.UNKNOWN;
        }
    }
}
