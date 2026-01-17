package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.entity.EntityDespawnEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for EntityDespawnTranslator.
 * Uses hex dump files for packet data.
 */
class EntityDespawnTranslatorTest extends TranslatorTestBase {
    
    private static final int ENTITY_DESPAWN_OPCODE = 0x3017;
    private EntityDespawnTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new EntityDespawnTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse entity despawn from hex dump")
    void testEntityDespawn() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/entity/despawn_0x3017.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        EntityDespawnEvent event = assertSingleEvent(events, EntityDespawnEvent.class);
        assertEquals(12345, event.getEntityId());
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
            
            EntityDespawnEvent event = assertSingleEvent(events, EntityDespawnEvent.class);
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
        assertEquals(ENTITY_DESPAWN_OPCODE, translator.getOpcode());
    }
}
