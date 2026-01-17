package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for HPMPUpdateTranslator.
 * Uses hex dump files for packet data.
 */
class HPMPUpdateTranslatorTest extends TranslatorTestBase {
    
    private static final int HPMP_UPDATE_OPCODE = 0x3057;
    private HPMPUpdateTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new HPMPUpdateTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse HP update from hex dump")
    void testHPUpdate() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/combat/hp_update_0x3057.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        EntityHPMPUpdateEvent event = assertSingleEvent(events, EntityHPMPUpdateEvent.class);
        assertEquals(12345, event.getEntityId());
        assertEquals(EntityHPMPUpdateEvent.ChangeType.HP_CHANGED, event.getChangeType());
        assertNotNull(event.getNewHP());
        assertEquals(5000, event.getNewHP().intValue());
        assertNull(event.getNewMP());
    }
    
    @Test
    @DisplayName("Should parse MP update packet")
    void testMPUpdate() {
        var packet = packetBuilder()
            .putInt(54321)
            .putShort((short) 0)
            .putByte(0x02)      // MP_CHANGED
            .putInt(3000)       // newMP
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        EntityHPMPUpdateEvent event = assertSingleEvent(events, EntityHPMPUpdateEvent.class);
        assertEquals(EntityHPMPUpdateEvent.ChangeType.MP_CHANGED, event.getChangeType());
        assertNotNull(event.getNewMP());
        assertEquals(3000, event.getNewMP().intValue());
        assertNull(event.getNewHP());
    }
    
    @Test
    @DisplayName("Should parse HP and MP update packet")
    void testHPAndMPUpdate() {
        var packet = packetBuilder()
            .putInt(99999)
            .putShort((short) 0)
            .putByte(0x03)      // HP_AND_MP_CHANGED
            .putInt(8000)       // newHP
            .putInt(5000)       // newMP
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        EntityHPMPUpdateEvent event = assertSingleEvent(events, EntityHPMPUpdateEvent.class);
        assertEquals(EntityHPMPUpdateEvent.ChangeType.HP_AND_MP_CHANGED, event.getChangeType());
        assertNotNull(event.getNewHP());
        assertNotNull(event.getNewMP());
        assertEquals(8000, event.getNewHP().intValue());
        assertEquals(5000, event.getNewMP().intValue());
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
        assertEquals(HPMP_UPDATE_OPCODE, translator.getOpcode());
    }
}
