package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.ChunkedPacketManagerRegistry;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.character.CharacterDataBeginEvent;
import org.sokybot.gameevents.framework.ChunkedPacketManagerTestHelper;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for CharacterDataBeginTranslator.
 * Tests chunk manager registration and transaction initiation.
 */
class CharacterDataBeginTranslatorTest extends TranslatorTestBase {
    
    private static final int CHAR_DATA_BEGIN_OPCODE = 0x34A5;
    private CharacterDataBeginTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new CharacterDataBeginTranslator(createNullLookup());
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Clean up registry
        ChunkedPacketManagerRegistry.getInstance().unregister(TEST_MACHINE_NAME);
    }
    
    @Test
    @DisplayName("Should emit CharacterDataBeginEvent")
    void testEmitsEvent() {
        // Register chunk manager
        try (ChunkedPacketManagerTestHelper.AutoCloseableHelper helper = 
             new ChunkedPacketManagerTestHelper(TEST_MACHINE_NAME).autoCloseable()) {
            
            // Empty packet - BEGIN packet has no data
            var packet = packetBuilder().build();
            
            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
            
            CharacterDataBeginEvent event = assertSingleEvent(events, CharacterDataBeginEvent.class);
            assertEquals(TEST_MACHINE_NAME, event.getFullName());
        }
    }
    
    @Test
    @DisplayName("Should initialize chunk manager transaction")
    void testInitializesChunkManager() {
        ChunkedPacketManagerTestHelper helper = new ChunkedPacketManagerTestHelper(TEST_MACHINE_NAME);
        helper.register();
        
        try {
            var packet = packetBuilder().build();
            
            translator.translate(TEST_MACHINE_NAME, packet, null);
            
            // Verify transaction is active
            helper.assertTransactionActive(CHAR_DATA_BEGIN_OPCODE);
        } finally {
            helper.unregister();
        }
    }
    
    @Test
    @DisplayName("Should work without registered chunk manager")
    void testWithoutChunkManager() {
        // No chunk manager registered
        var packet = packetBuilder().build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        // Should still emit event
        assertSingleEvent(events, CharacterDataBeginEvent.class);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(CHAR_DATA_BEGIN_OPCODE, translator.getOpcode());
    }
}
