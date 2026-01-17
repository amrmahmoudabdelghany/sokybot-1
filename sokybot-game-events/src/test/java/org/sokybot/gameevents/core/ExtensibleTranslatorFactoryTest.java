package org.sokybot.gameevents.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.CoreTranslatorProvider;
import org.sokybot.gameevents.ExtensibleTranslatorFactory;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.core.ITranslatorProvider;
import org.sokybot.gameevents.framework.MockGameDataLookup;

/**
 * Tests for ExtensibleTranslatorFactory.
 * Tests provider registration, translator creation, and priority resolution.
 */
class ExtensibleTranslatorFactoryTest {
    
    private ExtensibleTranslatorFactory factory;
    private MockGameDataLookup lookup;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        factory = new ExtensibleTranslatorFactory();
        lookup = new MockGameDataLookup();
        // Activate factory
        factory.activate();
    }
    
    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        factory.deactivate();
    }
    
    @Test
    @DisplayName("Should create translators when provider is registered")
    void testCreateTranslatorsWithProvider() {
        // Register core provider manually (simulating OSGi binding)
        CoreTranslatorProvider provider = new CoreTranslatorProvider();
        factory.bindProvider(provider);
        
        Map<Integer, IPacketTranslator> translators = factory.createTranslators(lookup, null);
        
        assertNotNull(translators);
        assertFalse(translators.isEmpty());
        
        // Verify some core translators are created
        assertTrue(translators.containsKey(0x3015)); // EntitySpawn
        assertTrue(translators.containsKey(0x3016)); // EntityDespawn
        assertTrue(translators.containsKey(0xA103)); // AuthResponse
    }
    
    @Test
    @DisplayName("Should return empty map when no providers registered")
    void testCreateTranslatorsNoProviders() {
        Map<Integer, IPacketTranslator> translators = factory.createTranslators(lookup, null);
        
        assertNotNull(translators);
        assertTrue(translators.isEmpty());
    }
    
    @Test
    @DisplayName("Should use highest priority provider for opcodes")
    void testProviderPriority() {
        // Create a mock high-priority provider
        MockTranslatorProvider highPriorityProvider = new MockTranslatorProvider(200, Set.of(0x3015));
        MockTranslatorProvider lowPriorityProvider = new MockTranslatorProvider(50, Set.of(0x3015));
        
        factory.bindProvider(lowPriorityProvider);
        factory.bindProvider(highPriorityProvider); // Higher priority, should be used
        
        Map<Integer, IPacketTranslator> translators = factory.createTranslators(lookup, null);
        
        assertNotNull(translators.get(0x3015));
        assertEquals(1, highPriorityProvider.createCount);
        assertEquals(0, lowPriorityProvider.createCount); // Should not be used
    }
    
    @Test
    @DisplayName("Should handle multiple providers with different opcodes")
    void testMultipleProviders() {
        CoreTranslatorProvider coreProvider = new CoreTranslatorProvider();
        MockTranslatorProvider customProvider = new MockTranslatorProvider(150, Set.of(0x9999));
        
        factory.bindProvider(coreProvider);
        factory.bindProvider(customProvider);
        
        Map<Integer, IPacketTranslator> translators = factory.createTranslators(lookup, null);
        
        // Should have translators from both providers
        assertTrue(translators.containsKey(0x3015)); // Core provider
        assertTrue(translators.containsKey(0x9999)); // Custom provider
    }
    
    @Test
    @DisplayName("Should handle provider unregistration")
    void testProviderUnregistration() {
        CoreTranslatorProvider provider = new CoreTranslatorProvider();
        factory.bindProvider(provider);
        
        Map<Integer, IPacketTranslator> translators = factory.createTranslators(lookup, null);
        assertFalse(translators.isEmpty());
        
        factory.unbindProvider(provider);
        
        Map<Integer, IPacketTranslator> translatorsAfter = factory.createTranslators(lookup, null);
        assertTrue(translatorsAfter.isEmpty());
    }
    
    @Test
    @DisplayName("Should create same translator instance for same game (per-game scope)")
    void testPerGameTranslators() {
        CoreTranslatorProvider provider = new CoreTranslatorProvider();
        factory.bindProvider(provider);
        
        Map<Integer, IPacketTranslator> translators1 = factory.createTranslators(lookup, null);
        Map<Integer, IPacketTranslator> translators2 = factory.createTranslators(lookup, null);
        
        // Should create new instances each time (translators are created per-game call)
        // But should have same opcodes
        assertEquals(translators1.keySet(), translators2.keySet());
        
        // Each call creates new instances (not cached in factory)
        // This is expected - caching happens in GroupContext
    }
    
    @Test
    @DisplayName("Should handle provider without getSupportedOpcodes")
    void testProviderWithoutSupportedOpcodes() {
        // Create provider that returns null for getSupportedOpcodes
        MockTranslatorProvider provider = new MockTranslatorProvider(100, null);
        factory.bindProvider(provider);
        
        Map<Integer, IPacketTranslator> translators = factory.createTranslators(lookup, null);
        
        // Should handle gracefully (won't create translators since no opcodes known)
        assertTrue(translators.isEmpty());
    }
    
    /**
     * Mock translator provider for testing.
     */
    private static class MockTranslatorProvider implements ITranslatorProvider {
        private final int priority;
        private final Set<Integer> supportedOpcodes;
        int createCount = 0;
        
        MockTranslatorProvider(int priority, Set<Integer> supportedOpcodes) {
            this.priority = priority;
            this.supportedOpcodes = supportedOpcodes;
        }
        
        @Override
        public boolean supports(int opcode, org.sokybot.persistence.service.IGameDataLookup lookup) {
            return supportedOpcodes != null && supportedOpcodes.contains(opcode);
        }
        
        @Override
        public IPacketTranslator createTranslator(int opcode, org.sokybot.persistence.service.IGameDataLookup lookup) {
            createCount++;
            // Return a dummy translator
            return new org.sokybot.gameevents.events.core.IPacketTranslator() {
                @Override
                public int getOpcode() {
                    return opcode;
                }
                
                @Override
                public java.util.List<org.sokybot.gameevents.events.core.IGameEvent> translate(
                        String machineFullName, 
                        org.sokybot.network.packet.ImmutablePacket packet,
                        org.sokybot.gameevents.ChunkedPacketManager chunkManager) {
                    return java.util.Collections.emptyList();
                }
            };
        }
        
        @Override
        public int getPriority() {
            return priority;
        }
        
        @Override
        public Set<Integer> getSupportedOpcodes() {
            return supportedOpcodes;
        }
    }
}
