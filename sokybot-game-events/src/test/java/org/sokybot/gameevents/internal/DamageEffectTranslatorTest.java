package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.buff.DamageEffectEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for DamageEffectTranslator.
 * Uses hex dump files for packet data.
 */
class DamageEffectTranslatorTest extends TranslatorTestBase {
    
    private static final int DAMAGE_EFFECT_OPCODE = 0x3058;
    private DamageEffectTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new DamageEffectTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse damage effect from hex dump")
    void testDamageEffect() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/combat/damage_effect_0x3058.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        DamageEffectEvent event = assertSingleEvent(events, DamageEffectEvent.class);
        assertEquals(12345, event.getTargetEntityId());
        assertEquals(2500, event.getDamageAmount());
        assertEquals(DamageEffectEvent.DamageType.NORMAL, event.getDamageType());
        assertEquals(TEST_MACHINE_NAME, event.getFullName());
    }
    
    @Test
    @DisplayName("Should parse critical damage from hex dump")
    void testCriticalDamage() throws Exception {
        // Load packet from hex dump file
        var packet = loadHexDump("packets/combat/damage_effect_critical_0x3058.hex");
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        DamageEffectEvent event = assertSingleEvent(events, DamageEffectEvent.class);
        assertEquals(DamageEffectEvent.DamageType.CRITICAL, event.getDamageType());
        assertEquals(5000, event.getDamageAmount());
        assertEquals(54321, event.getTargetEntityId());
    }
    
    @Test
    @DisplayName("Should handle different damage amounts")
    void testDifferentDamageAmounts() {
        int[] damages = { 1, 100, 1000, 10000, 999999 };
        
        for (int damage : damages) {
            var packet = packetBuilder()
                .putInt(12345)
                .putByte(0x00)
                .putInt(damage)
                .build();
            
            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
            
            DamageEffectEvent event = assertSingleEvent(events, DamageEffectEvent.class);
            assertEquals(damage, event.getDamageAmount());
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
        assertEquals(DAMAGE_EFFECT_OPCODE, translator.getOpcode());
    }
}
