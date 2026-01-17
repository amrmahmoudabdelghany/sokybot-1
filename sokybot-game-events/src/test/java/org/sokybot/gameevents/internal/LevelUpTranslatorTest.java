package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.stat.LevelUpEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for LevelUpTranslator.
 * Uses hex dump files for packet data.
 */
class LevelUpTranslatorTest extends TranslatorTestBase {
    
    private static final int LEVEL_UP_OPCODE = 0x3054;
    private LevelUpTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new LevelUpTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse level up from hex dump")
    void testLevelUp() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/stat/level_up_0x3054.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        LevelUpEvent event = assertSingleEvent(events, LevelUpEvent.class);
        assertEquals(54321, event.getEntityId());
        assertEquals(TEST_MACHINE_NAME, event.getFullName());
    }
    
    @Test
    @DisplayName("Should handle different entity IDs")
    void testDifferentEntityIds() {
        int[] entityIds = { 1, 100, 1000, 10000, 999999 };
        
        for (int entityId : entityIds) {
            var packet = packetBuilder()
                .putInt(entityId)
                .build();
            
            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
            
            LevelUpEvent event = assertSingleEvent(events, LevelUpEvent.class);
            assertEquals(entityId, event.getEntityId());
        }
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
        assertEquals(LEVEL_UP_OPCODE, translator.getOpcode());
    }
}
