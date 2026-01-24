package org.sokybot.gameevents.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.ChunkedPacketManagerRegistry;

/**
 * Tests for ChunkedPacketManagerRegistry.
 */
class ChunkedPacketManagerRegistryTest {
    
    private static final String MACHINE_1 = "test.group.bot1";
    private static final String MACHINE_2 = "test.group.bot2";
    
    private ChunkedPacketManagerRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = ChunkedPacketManagerRegistry.getInstance();
        // Clean up any existing registrations from previous tests
        registry.unregister(MACHINE_1);
        registry.unregister(MACHINE_2);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up after each test
        registry.unregister(MACHINE_1);
        registry.unregister(MACHINE_2);
    }
    
    @Test
    @DisplayName("Should register chunk manager successfully")
    void testRegister() {
        ChunkedPacketManager manager = new ChunkedPacketManager();
        
        registry.register(MACHINE_1, manager);
        
        assertTrue(registry.isRegistered(MACHINE_1));
        assertEquals(1, registry.size());
    }
    
    @Test
    @DisplayName("Should retrieve registered chunk manager")
    void testGet() {
        ChunkedPacketManager manager = new ChunkedPacketManager();
        registry.register(MACHINE_1, manager);
        
        ChunkedPacketManager retrieved = registry.get(MACHINE_1);
        
        assertNotNull(retrieved);
        assertEquals(manager, retrieved);
    }
    
    @Test
    @DisplayName("Should return null for unregistered machine")
    void testGetUnregistered() {
        ChunkedPacketManager retrieved = registry.get(MACHINE_1);
        
        assertNull(retrieved);
        assertFalse(registry.isRegistered(MACHINE_1));
    }
    
    @Test
    @DisplayName("Should unregister chunk manager")
    void testUnregister() {
        ChunkedPacketManager manager = new ChunkedPacketManager();
        registry.register(MACHINE_1, manager);
        assertTrue(registry.isRegistered(MACHINE_1));
        
        registry.unregister(MACHINE_1);
        
        assertFalse(registry.isRegistered(MACHINE_1));
        assertNull(registry.get(MACHINE_1));
        assertEquals(0, registry.size());
    }
    
    @Test
    @DisplayName("Should handle unregister of non-existent machine gracefully")
    void testUnregisterNonExistent() {
        // Should not throw exception
        registry.unregister("non-existent");
        
        assertEquals(0, registry.size());
    }
    
    @Test
    @DisplayName("Should handle null machine name in unregister gracefully")
    void testUnregisterNull() {
        // Should not throw exception
        registry.unregister(null);
        
        assertEquals(0, registry.size());
    }
    
    @Test
    @DisplayName("Should support multiple machines")
    void testMultipleMachines() {
        ChunkedPacketManager manager1 = new ChunkedPacketManager();
        ChunkedPacketManager manager2 = new ChunkedPacketManager();
        
        registry.register(MACHINE_1, manager1);
        registry.register(MACHINE_2, manager2);
        
        assertEquals(2, registry.size());
        assertTrue(registry.isRegistered(MACHINE_1));
        assertTrue(registry.isRegistered(MACHINE_2));
        assertEquals(manager1, registry.get(MACHINE_1));
        assertEquals(manager2, registry.get(MACHINE_2));
    }
    
    @Test
    @DisplayName("Should allow re-registration (replacing existing)")
    void testReregister() {
        ChunkedPacketManager manager1 = new ChunkedPacketManager();
        ChunkedPacketManager manager2 = new ChunkedPacketManager();
        
        registry.register(MACHINE_1, manager1);
        assertEquals(manager1, registry.get(MACHINE_1));
        
        registry.register(MACHINE_1, manager2);
        
        assertEquals(1, registry.size());
        assertEquals(manager2, registry.get(MACHINE_1));
    }
    
    @Test
    @DisplayName("Should throw exception when registering null machine name")
    void testRegisterNullMachineName() {
        ChunkedPacketManager manager = new ChunkedPacketManager();
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(null, manager);
        });
    }
    
    @Test
    @DisplayName("Should throw exception when registering null chunk manager")
    void testRegisterNullChunkManager() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(MACHINE_1, null);
        });
    }
    
    @Test
    @DisplayName("Should return correct size")
    void testSize() {
        assertEquals(0, registry.size());
        
        registry.register(MACHINE_1, new ChunkedPacketManager());
        assertEquals(1, registry.size());
        
        registry.register(MACHINE_2, new ChunkedPacketManager());
        assertEquals(2, registry.size());
        
        registry.unregister(MACHINE_1);
        assertEquals(1, registry.size());
        
        registry.unregister(MACHINE_2);
        assertEquals(0, registry.size());
    }
    
    @Test
    @DisplayName("Should be thread-safe for concurrent access")
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int registrationsPerThread = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < registrationsPerThread; j++) {
                    String machineName = "thread-" + threadId + "-bot-" + j;
                    ChunkedPacketManager manager = new ChunkedPacketManager();
                    registry.register(machineName, manager);
                    assertNotNull(registry.get(machineName));
                    registry.unregister(machineName);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(0, registry.size());
    }
}
