package org.sokybot.gameevents.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.framework.formats.HexDumpPacketFormat;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Base class for translator tests.
 * Provides common utilities and assertion helpers.
 */
public abstract class TranslatorTestBase {
    
    protected static final String TEST_MACHINE_NAME = "test.group.bot";
    
    /**
     * Create a packet from byte array.
     */
    protected ImmutablePacket createPacket(byte[] data) {
        return ImmutablePacket.wrap(data);
    }
    
    /**
     * Create a packet using PacketTestBuilder.
     */
    protected PacketTestBuilder packetBuilder() {
        return PacketTestBuilder.create();
    }
    
    /**
     * Create a packet using PacketTestBuilder with initial capacity.
     */
    protected PacketTestBuilder packetBuilder(int capacity) {
        return PacketTestBuilder.create(capacity);
    }
    
    /**
     * Create a mock game data lookup.
     */
    protected MockGameDataLookup createLookup() {
        return new MockGameDataLookup();
    }
    
    /**
     * Create a null lookup (for translators that don't need game data).
     */
    protected IGameDataLookup createNullLookup() {
        return null;
    }
    
    /**
     * Load a packet from a hex dump file resource.
     * Convenience method for tests that use hex dump files.
     * 
     * @param resourcePath Classpath resource path (e.g., "packets/auth/success_0xA103.hex")
     * @return ImmutablePacket instance
     * @throws Exception If the resource cannot be found or read
     */
    protected ImmutablePacket loadHexDump(String resourcePath) throws Exception {
        PacketDataLoader loader = new HexDumpPacketFormat();
        return loader.loadPacketFromResource(resourcePath);
    }
    
    /**
     * Parse a hex string directly (useful for inline test data).
     * 
     * @param hexString Hex string with or without spaces (e.g., "A103" or "A1 03")
     * @return ImmutablePacket instance
     */
    protected ImmutablePacket parseHexString(String hexString) {
        return HexDumpPacketFormat.parseHexString(hexString);
    }
    
    /**
     * Assert that a list contains exactly one event.
     */
    protected <T extends IGameEvent> T assertSingleEvent(List<IGameEvent> events, Class<T> expectedType) {
        assertNotNull(events, "Events list should not be null");
        assertFalse(events.isEmpty(), "Events list should not be empty");
        assertEquals(1, events.size(), "Should have exactly one event");
        
        IGameEvent event = events.get(0);
        assertNotNull(event, "Event should not be null");
        assertTrue(expectedType.isInstance(event), 
                   "Event should be of type " + expectedType.getSimpleName() + " but was " + event.getClass().getSimpleName());
        
        assertEquals(TEST_MACHINE_NAME, event.getFullName(), "Event should have correct machine name");
        
        return expectedType.cast(event);
    }
    
    /**
     * Assert that events list is empty.
     */
    protected void assertNoEvents(List<IGameEvent> events) {
        assertNotNull(events, "Events list should not be null");
        assertTrue(events.isEmpty(), "Events list should be empty");
    }
    
    /**
     * Assert that an event has specific field values.
     * Uses reflection to check field values.
     * 
     * @param event The event to check
     * @param expectedValues Map of field names to expected values
     */
    protected void assertEventFields(IGameEvent event, Map<String, Object> expectedValues) {
        assertNotNull(event, "Event should not be null");
        
        for (Map.Entry<String, Object> entry : expectedValues.entrySet()) {
            String fieldName = entry.getKey();
            Object expectedValue = entry.getValue();
            
            try {
                java.lang.reflect.Field field = event.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object actualValue = field.get(event);
                
                if (expectedValue == null) {
                    assertTrue(actualValue == null, 
                              "Field " + fieldName + " should be null");
                } else if (expectedValue instanceof Float || expectedValue instanceof Double) {
                    // Use approximate comparison for floating point
                    double expected = ((Number) expectedValue).doubleValue();
                    double actual = ((Number) actualValue).doubleValue();
                    assertEquals(expected, actual, 0.0001, 
                                "Field " + fieldName + " should match");
                } else {
                    assertEquals(expectedValue, actualValue, 
                                "Field " + fieldName + " should match");
                }
            } catch (NoSuchFieldException e) {
                throw new AssertionError("Field " + fieldName + " not found in " + event.getClass().getSimpleName(), e);
            } catch (IllegalAccessException e) {
                throw new AssertionError("Cannot access field " + fieldName, e);
            }
        }
    }
    
    /**
     * Assert that events list contains at least N events.
     */
    protected void assertEventCount(List<IGameEvent> events, int expectedCount) {
        assertNotNull(events, "Events list should not be null");
        assertEquals(expectedCount, events.size(), 
                    "Should have exactly " + expectedCount + " events");
    }
    
    /**
     * Assert that events list contains at least N events.
     */
    protected void assertMinEventCount(List<IGameEvent> events, int minCount) {
        assertNotNull(events, "Events list should not be null");
        assertTrue(events.size() >= minCount, 
                  "Should have at least " + minCount + " events, but has " + events.size());
    }
}
