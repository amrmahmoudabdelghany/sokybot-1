package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.ChunkedPacketManagerRegistry;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.framework.ChunkedPacketManagerTestHelper;
import org.sokybot.gameevents.framework.PacketTestBuilder;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for CharacterDataChunkTranslator.
 * Tests chunk accumulation in chunked packet transactions.
 */
class CharacterDataChunkTranslatorTest extends TranslatorTestBase {
    
    private static final int CHAR_DATA_CHUNK_OPCODE = 0x3013;
    private static final int CHAR_DATA_BEGIN_OPCODE = 0x34A5;
    private CharacterDataChunkTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new CharacterDataChunkTranslator(createNullLookup());
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        ChunkedPacketManagerRegistry.getInstance().unregister(TEST_MACHINE_NAME);
    }
    
    @Test
    @DisplayName("Should return empty events list")
    void testReturnsNoEvents() {
        ChunkedPacketManagerTestHelper helper = new ChunkedPacketManagerTestHelper(TEST_MACHINE_NAME);
        helper.register();
        
        try {
            // Start transaction
            helper.getChunkManager().begin(CHAR_DATA_BEGIN_OPCODE);
            
            // Process chunk packet
            var packet = packetBuilder()
                .putBytes(new byte[] { 0x01, 0x02, 0x03, 0x04 })
                .build();
            
            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
            
            // Chunk translator should not emit events
            assertNoEvents(events);
        } finally {
            helper.unregister();
        }
    }
    
    @Test
    @DisplayName("Should accumulate chunk data when transaction is active")
    void testAccumulatesChunks() {
        ChunkedPacketManagerTestHelper helper = new ChunkedPacketManagerTestHelper(TEST_MACHINE_NAME);
        helper.register();
        
        try {
            // Start transaction
            helper.getChunkManager().begin(CHAR_DATA_BEGIN_OPCODE);
            
            // Process chunk packet
            byte[] chunkData = new byte[] { 0x01, 0x02, 0x03, 0x04 };
            var packet = PacketTestBuilder.create().putBytes(chunkData).build();
            
            translator.translate(TEST_MACHINE_NAME, packet, null);
            
            // Verify transaction is still active (chunks accumulate)
            helper.assertTransactionActive(CHAR_DATA_BEGIN_OPCODE);
        } finally {
            helper.unregister();
        }
    }
    
    @Test
    @DisplayName("Should ignore chunks when transaction is not active")
    void testIgnoresChunksWithoutActiveTransaction() {
        ChunkedPacketManagerTestHelper helper = new ChunkedPacketManagerTestHelper(TEST_MACHINE_NAME);
        helper.register();
        
        try {
            // No transaction started
            
            var packet = packetBuilder()
                .putBytes(new byte[] { 0x01, 0x02, 0x03, 0x04 })
                .build();
            
            List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
            
            // Should return empty (no transaction active)
            assertNoEvents(events);
        } finally {
            helper.unregister();
        }
    }
    
    @Test
    @DisplayName("Should work without registered chunk manager")
    void testWithoutChunkManager() {
        // No chunk manager registered
        var packet = packetBuilder()
            .putBytes(new byte[] { 0x01, 0x02 })
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        // Should return empty gracefully
        assertNoEvents(events);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(CHAR_DATA_CHUNK_OPCODE, translator.getOpcode());
    }
}
