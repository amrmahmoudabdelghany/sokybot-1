package org.sokybot.gameevents.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.CoreTranslatorProvider;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.framework.MockGameDataLookup;

/**
 * Tests for CoreTranslatorProvider.
 */
class CoreTranslatorProviderTest {
    
    private CoreTranslatorProvider provider;
    private MockGameDataLookup lookup;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        provider = new CoreTranslatorProvider();
        lookup = new MockGameDataLookup();
    }
    
    @Test
    @DisplayName("Should have correct priority (100)")
    void testPriority() {
        assertEquals(100, provider.getPriority());
    }
    
    @Test
    @DisplayName("Should support core opcodes")
    void testSupportsCoreOpcodes() {
        // Test a few key opcodes
        assertTrue(provider.supports(0x3015, lookup)); // EntitySpawn
        assertTrue(provider.supports(0x3016, lookup)); // EntityDespawn
        assertTrue(provider.supports(0xA103, lookup)); // AuthResponse
        assertTrue(provider.supports(0x34A5, lookup)); // CharacterDataBegin
        assertTrue(provider.supports(0x34A6, lookup)); // CharacterDataEnd
        assertTrue(provider.supports(0x3013, lookup)); // CharacterDataChunk
    }
    
    @Test
    @DisplayName("Should not support unknown opcodes")
    void testDoesNotSupportUnknownOpcodes() {
        assertFalse(provider.supports(0x9999, lookup));
        assertFalse(provider.supports(0x0000, lookup));
        assertFalse(provider.supports(0xFFFF, lookup));
    }
    
    @Test
    @DisplayName("Should return supported opcodes set")
    void testGetSupportedOpcodes() {
        Set<Integer> supportedOpcodes = provider.getSupportedOpcodes();
        
        assertNotNull(supportedOpcodes);
        assertFalse(supportedOpcodes.isEmpty());
        
        // Verify some expected opcodes are included
        assertTrue(supportedOpcodes.contains(0x3015)); // EntitySpawn
        assertTrue(supportedOpcodes.contains(0x3016)); // EntityDespawn
        assertTrue(supportedOpcodes.contains(0xA103)); // AuthResponse
    }
    
    @Test
    @DisplayName("Should create EntitySpawnTranslator")
    void testCreateEntitySpawnTranslator() {
        IPacketTranslator translator = provider.createTranslator(0x3015, lookup);
        
        assertNotNull(translator);
        assertEquals(0x3015, translator.getOpcode());
    }
    
    @Test
    @DisplayName("Should create AuthResponseTranslator")
    void testCreateAuthResponseTranslator() {
        IPacketTranslator translator = provider.createTranslator(0xA103, lookup);
        
        assertNotNull(translator);
        assertEquals(0xA103, translator.getOpcode());
    }
    
    @Test
    @DisplayName("Should create CharacterDataBeginTranslator")
    void testCreateCharacterDataBeginTranslator() {
        IPacketTranslator translator = provider.createTranslator(0x34A5, lookup);
        
        assertNotNull(translator);
        assertEquals(0x34A5, translator.getOpcode());
    }
    
    @Test
    @DisplayName("Should create CharacterDataChunkTranslator")
    void testCreateCharacterDataChunkTranslator() {
        IPacketTranslator translator = provider.createTranslator(0x3013, lookup);
        
        assertNotNull(translator);
        assertEquals(0x3013, translator.getOpcode());
    }
    
    @Test
    @DisplayName("Should create CharacterDataEndTranslator")
    void testCreateCharacterDataEndTranslator() {
        IPacketTranslator translator = provider.createTranslator(0x34A6, lookup);
        
        assertNotNull(translator);
        assertEquals(0x34A6, translator.getOpcode());
    }
    
    @Test
    @DisplayName("Should return null for unsupported opcode")
    void testCreateTranslatorUnsupportedOpcode() {
        IPacketTranslator translator = provider.createTranslator(0x9999, lookup);
        
        assertNull(translator);
    }
    
    @Test
    @DisplayName("Should handle null lookup gracefully")
    void testCreateTranslatorWithNullLookup() {
        IPacketTranslator translator = provider.createTranslator(0x3015, null);
        
        assertNotNull(translator);
        assertEquals(0x3015, translator.getOpcode());
    }
    
    @Test
    @DisplayName("Should support all opcodes returned by getSupportedOpcodes")
    void testAllSupportedOpcodesAreSupported() {
        Set<Integer> supportedOpcodes = provider.getSupportedOpcodes();
        
        assertNotNull(supportedOpcodes);
        assertFalse(supportedOpcodes.isEmpty());
        
        for (Integer opcode : supportedOpcodes) {
            assertTrue(provider.supports(opcode, lookup),
                      "Opcode 0x" + Integer.toHexString(opcode) + " should be supported");
        }
    }
}
