package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.session.LoginRequestEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for LoginRequestTranslator.
 * Uses hex dump files for packet data.
 */
class LoginRequestTranslatorTest extends TranslatorTestBase {
    
    private static final int LOGIN_REQUEST_OPCODE = 0x6102;
    private LoginRequestTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new LoginRequestTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse login request from hex dump")
    void testLoginRequest() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/session/login_request_0x6102.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        LoginRequestEvent event = assertSingleEvent(events, LoginRequestEvent.class);
        assertEquals("testuser", event.getUsername());
        assertEquals("testpass123", event.getPassword());
        assertEquals(TEST_MACHINE_NAME, event.getFullName());
    }
    
    @Test
    @DisplayName("Should handle empty username and password")
    void testEmptyCredentials() {
        // Use inline hex for edge case: local + empty strings
        var packet = parseHexString("00 00 00 00 00");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        LoginRequestEvent event = assertSingleEvent(events, LoginRequestEvent.class);
        assertEquals("", event.getUsername());
        assertEquals("", event.getPassword());
    }
    
    @Test
    @DisplayName("Should handle long username and password")
    void testLongCredentials() {
        // Use packet builder for variable-length data
        String longUsername = "a".repeat(100);
        String longPassword = "b".repeat(100);
        
        var packet = packetBuilder()
            .putByte(0x00)
            .putString(longUsername)
            .putString(longPassword)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        LoginRequestEvent event = assertSingleEvent(events, LoginRequestEvent.class);
        assertEquals(longUsername, event.getUsername());
        assertEquals(longPassword, event.getPassword());
    }
    
    @Test
    @DisplayName("Should return empty list on malformed packet")
    void testMalformedPacket() {
        var packet = createPacket(new byte[] { 0x01, 0x02 }); // Too short
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        assertNoEvents(events);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(LOGIN_REQUEST_OPCODE, translator.getOpcode());
    }
}
