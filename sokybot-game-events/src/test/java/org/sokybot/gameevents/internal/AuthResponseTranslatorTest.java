package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.session.AuthResponseEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for AuthResponseTranslator.
 * Uses hex dump files for packet data.
 */
class AuthResponseTranslatorTest extends TranslatorTestBase {
    
    private static final int AUTH_RESPONSE_OPCODE = 0xA103;
    private AuthResponseTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new AuthResponseTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse successful auth response from hex dump")
    void testSuccessfulAuth() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/auth/success_0xA103.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        AuthResponseEvent event = assertSingleEvent(events, AuthResponseEvent.class);
        assertTrue(event.isSuccess());
        assertEquals(0x01, event.getResultCode());
    }
    
    @Test
    @DisplayName("Should parse failed auth response from hex dump")
    void testFailedAuth() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/auth/failure_0xA103.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        AuthResponseEvent event = assertSingleEvent(events, AuthResponseEvent.class);
        assertFalse(event.isSuccess());
        assertEquals(0x02, event.getResultCode());
    }
    
    @Test
    @DisplayName("Should parse other result codes as success")
    void testOtherResultCodes() {
        // Use inline hex string for additional test cases
        var packet = parseHexString("03");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        AuthResponseEvent event = assertSingleEvent(events, AuthResponseEvent.class);
        assertTrue(event.isSuccess());
        assertEquals(0x03, event.getResultCode());
    }
    
    @Test
    @DisplayName("Should return empty list on malformed packet")
    void testMalformedPacket() {
        var packet = createPacket(new byte[0]); // Empty packet
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        assertNoEvents(events);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(AUTH_RESPONSE_OPCODE, translator.getOpcode());
    }
}
