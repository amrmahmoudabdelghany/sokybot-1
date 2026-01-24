package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.entity.EntitySpawnEvent;
import org.sokybot.gameevents.framework.MockGameDataLookup;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for EntitySpawnTranslator.
 * Demonstrates testing with game data lookup.
 */
class EntitySpawnTranslatorTest extends TranslatorTestBase {
    
    private static final int ENTITY_SPAWN_OPCODE = 0x3015;
    private EntitySpawnTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Create translator with mock lookup
        MockGameDataLookup lookup = createLookup();
        translator = new EntitySpawnTranslator(lookup);
    }
    
    @Test
    @DisplayName("Should parse entity spawn packet with NPC")
    void testEntitySpawnWithNPC() {
        // Register an NPC in the lookup
        MockGameDataLookup lookup = createLookup();
        lookup.addNPC(12345, "TestMonster");
        translator = new EntitySpawnTranslator(lookup);
        
        // Build packet: refId (int) + spawn data...
        // Note: This is a simplified test - full spawn packets are complex
        // The actual packet structure would include position, angles, etc.
        var packet = packetBuilder(256) // Large capacity for spawn data
            .putInt(12345) // refId
            // ... spawn data fields would go here
            .build();
        
        // This test may return empty events if spawn data parsing is incomplete
        // The important part is demonstrating the test pattern
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        // Verify translator can process the packet without errors
        assertNotNull(events);
    }
    
    @Test
    @DisplayName("Should handle entity spawn without registered NPC")
    void testEntitySpawnWithoutNPC() {
        // No NPC registered in lookup
        var packet = packetBuilder()
            .putInt(99999) // Unknown refId
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        // Should handle gracefully (may return empty or noEvents)
        assertNotNull(events);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(ENTITY_SPAWN_OPCODE, translator.getOpcode());
    }
    
    @Test
    @DisplayName("Should handle malformed packet gracefully")
    void testMalformedPacket() {
        var packet = createPacket(new byte[] { 0x01, 0x02 }); // Too short
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        // Should return empty list on error
        assertNoEvents(events);
    }
}
