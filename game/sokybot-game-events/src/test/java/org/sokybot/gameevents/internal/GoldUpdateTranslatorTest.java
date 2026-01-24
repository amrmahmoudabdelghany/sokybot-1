package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.stat.GoldUpdateEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for GoldUpdateTranslator.
 * Uses hex dump files for packet data.
 */
class GoldUpdateTranslatorTest extends TranslatorTestBase {
    
    private static final int ATTACK_GAINS_UPDATE_OPCODE = 0x304E;
    private static final byte GOLD_TYPE = 1;
    private GoldUpdateTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new GoldUpdateTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse gold update from hex dump")
    void testGoldUpdate() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/stat/gold_update_0x304E.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        GoldUpdateEvent event = assertSingleEvent(events, GoldUpdateEvent.class);
        assertEquals(1000000L, event.getNewGoldAmount());
        assertEquals(TEST_MACHINE_NAME, event.getFullName());
    }
    
    @Test
    @DisplayName("Should handle different gold amounts")
    void testDifferentGoldAmounts() {
        long[] amounts = { 0L, 100L, 10000L, 1000000L, Long.MAX_VALUE };
        
        for (long amount : amounts) {
            var packet = packetBuilder()
                .putByte(GOLD_TYPE)
                .putLong(amount)
                .build();
            
            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
            
            GoldUpdateEvent event = assertSingleEvent(events, GoldUpdateEvent.class);
            assertEquals(amount, event.getNewGoldAmount());
        }
    }
    
    @Test
    @DisplayName("Should return empty for non-gold type")
    void testNonGoldType() {
        // SP type (2) or ZERK type (3)
        var packet = packetBuilder()
            .putByte((byte) 2) // SP type
            .putLong(5000L)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        // Should return empty for non-gold types
        assertNoEvents(events);
    }
    
    @Test
    @DisplayName("Should return empty list on malformed packet")
    void testMalformedPacket() {
        var packet = createPacket(new byte[] { GOLD_TYPE }); // Too short - missing long
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        assertNoEvents(events);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(ATTACK_GAINS_UPDATE_OPCODE, translator.getOpcode());
    }
}
