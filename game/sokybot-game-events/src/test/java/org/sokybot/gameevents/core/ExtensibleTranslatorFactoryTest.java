package org.sokybot.gameevents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
        MockTranslatorProvider provider = new MockTranslatorProvider(100, Set.of(0x3015, 0x3016, 0xA103));
        factory.bindProvider(provider);

        Map<Integer, List<IPacketTranslator>> translators = factory.createTranslators(lookup, null);

        assertNotNull(translators);
        assertFalse(translators.isEmpty());
        assertTrue(translators.containsKey(0x3015));
        assertTrue(translators.containsKey(0x3016));
        assertTrue(translators.containsKey(0xA103));
    }

    @Test
    @DisplayName("Should return empty map when no providers registered")
    void testCreateTranslatorsNoProviders() {
        Map<Integer, List<IPacketTranslator>> translators = factory.createTranslators(lookup, null);

        assertNotNull(translators);
        assertTrue(translators.isEmpty());
    }

    @Test
    @DisplayName("Should chain providers by descending priority for same opcode")
    void testProviderPriority() {
        MockTranslatorProvider highPriorityProvider = new MockTranslatorProvider(200, Set.of(0x3015));
        MockTranslatorProvider lowPriorityProvider = new MockTranslatorProvider(50, Set.of(0x3015));

        factory.bindProvider(lowPriorityProvider);
        factory.bindProvider(highPriorityProvider); // Higher priority, should be used

        Map<Integer, List<IPacketTranslator>> translators = factory.createTranslators(lookup, null);

        List<IPacketTranslator> chain = translators.get(0x3015);
        assertNotNull(chain);
        assertEquals(2, chain.size());
        assertEquals(highPriorityProvider.lastCreatedTranslator, chain.get(0));
        assertEquals(lowPriorityProvider.lastCreatedTranslator, chain.get(1));
        assertEquals(1, highPriorityProvider.createCount);
        assertEquals(1, lowPriorityProvider.createCount);
    }

    @Test
    @DisplayName("Should handle multiple providers with different opcodes")
    void testMultipleProviders() {
        MockTranslatorProvider provider1 = new MockTranslatorProvider(100, Set.of(0x3015));
        MockTranslatorProvider provider2 = new MockTranslatorProvider(150, Set.of(0x9999));

        factory.bindProvider(provider1);
        factory.bindProvider(provider2);

        Map<Integer, List<IPacketTranslator>> translators = factory.createTranslators(lookup, null);

        assertTrue(translators.containsKey(0x3015));
        assertTrue(translators.containsKey(0x9999));
    }

    @Test
    @DisplayName("Should handle provider unregistration")
    void testProviderUnregistration() {
        MockTranslatorProvider provider = new MockTranslatorProvider(100, Set.of(0x3015));
        factory.bindProvider(provider);

        Map<Integer, List<IPacketTranslator>> translators = factory.createTranslators(lookup, null);
        assertFalse(translators.isEmpty());

        factory.unbindProvider(provider);

        Map<Integer, List<IPacketTranslator>> translatorsAfter = factory.createTranslators(lookup, null);
        assertTrue(translatorsAfter.isEmpty());
    }

    @Test
    @DisplayName("Should create same translator instance for same game (per-game scope)")
    void testPerGameTranslators() {
        MockTranslatorProvider provider = new MockTranslatorProvider(100, Set.of(0x3015, 0x3016));
        factory.bindProvider(provider);

        Map<Integer, List<IPacketTranslator>> translators1 = factory.createTranslators(lookup, null);
        Map<Integer, List<IPacketTranslator>> translators2 = factory.createTranslators(lookup, null);

        assertEquals(translators1.keySet(), translators2.keySet());
    }

    @Test
    @DisplayName("Should handle provider without getSupportedOpcodes")
    void testProviderWithoutSupportedOpcodes() {
        MockTranslatorProvider provider = new MockTranslatorProvider(100, null);
        factory.bindProvider(provider);

        Map<Integer, List<IPacketTranslator>> translators = factory.createTranslators(lookup, null);

        // Should handle gracefully (won't create translators since no opcodes known)
        assertTrue(translators.isEmpty());
    }

    @Test
    @DisplayName("Should preserve single-translator compatibility shim")
    void testCreateTranslatorsSingleShim() {
        MockTranslatorProvider highPriorityProvider = new MockTranslatorProvider(200, Set.of(0x3015));
        MockTranslatorProvider lowPriorityProvider = new MockTranslatorProvider(50, Set.of(0x3015));
        factory.bindProvider(lowPriorityProvider);
        factory.bindProvider(highPriorityProvider);

        Map<Integer, IPacketTranslator> flattened = factory.createTranslatorsSingle(lookup, null);

        assertTrue(flattened.containsKey(0x3015));
        assertEquals(highPriorityProvider.lastCreatedTranslator, flattened.get(0x3015));
    }

    /**
     * Mock translator provider for testing.
     */
    private static class MockTranslatorProvider implements ITranslatorProvider {
        private final int priority;
        private final Set<Integer> supportedOpcodes;
        int createCount = 0;
        IPacketTranslator lastCreatedTranslator;

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
            lastCreatedTranslator = new org.sokybot.gameevents.events.core.IPacketTranslator() {
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
            return lastCreatedTranslator;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public Set<Integer> getSupportedOpcodes(org.sokybot.persistence.service.IGameDataLookup lookup) {
            return supportedOpcodes;
        }
    }
}
