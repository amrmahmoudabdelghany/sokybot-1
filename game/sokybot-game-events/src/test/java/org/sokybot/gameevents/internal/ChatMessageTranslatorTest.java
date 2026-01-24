package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.chat.ChatMessageEvent;
import org.sokybot.gameevents.events.chat.ChatMessageEvent.ChatType;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for ChatMessageTranslator.
 * Uses hex dump files for packet data.
 */
class ChatMessageTranslatorTest extends TranslatorTestBase {
    
    private static final int CHAT_UPDATE_OPCODE = 0x3026;
    private ChatMessageTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new ChatMessageTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse chat message from hex dump")
    void testChatMessage() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/chat/all_chat_0x3026.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        ChatMessageEvent event = assertSingleEvent(events, ChatMessageEvent.class);
        assertEquals(ChatType.ALL, event.getChatType());
        assertEquals("PlayerName", event.getSenderName());
        assertEquals("Hello world!", event.getMessage());
        assertEquals(TEST_MACHINE_NAME, event.getFullName());
    }
    
    @Test
    @DisplayName("Should handle different chat types")
    void testDifferentChatTypes() {
        ChatType[] chatTypes = {
            ChatType.ALL, ChatType.PRIVATE, ChatType.PARTY, 
            ChatType.GUILD, ChatType.GLOBAL, ChatType.NOTICE
        };
        byte[] chatTypeValues = { 1, 2, 3, 4, 5, 6 };
        
        for (int i = 0; i < chatTypes.length; i++) {
            var packet = packetBuilder()
                .putByte(chatTypeValues[i])
                .putString("Sender")
                .putString("Message")
                .build();
            
            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
            
            ChatMessageEvent event = assertSingleEvent(events, ChatMessageEvent.class);
            assertEquals(chatTypes[i], event.getChatType());
        }
    }
    
    @Test
    @DisplayName("Should handle empty message")
    void testEmptyMessage() {
        var packet = packetBuilder()
            .putByte(0x01)
            .putString("Sender")
            .putString("")
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        ChatMessageEvent event = assertSingleEvent(events, ChatMessageEvent.class);
        assertEquals("", event.getMessage());
    }
    
    @Test
    @DisplayName("Should handle unknown chat type")
    void testUnknownChatType() {
        var packet = packetBuilder()
            .putByte((byte) 99)  // Unknown chat type
            .putString("Sender")
            .putString("Message")
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        ChatMessageEvent event = assertSingleEvent(events, ChatMessageEvent.class);
        assertEquals(ChatType.UNKNOWN, event.getChatType());
    }
    
    @Test
    @DisplayName("Should return empty list on malformed packet")
    void testMalformedPacket() {
        var packet = createPacket(new byte[] { 0x01 }); // Too short
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        assertNoEvents(events);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(CHAT_UPDATE_OPCODE, translator.getOpcode());
    }
}
