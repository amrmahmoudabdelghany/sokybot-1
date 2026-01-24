package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.inventory.InventoryItemUpdateEvent;
import org.sokybot.gameevents.framework.TranslatorTestBase;

import java.util.List;

/**
 * Tests for InventoryItemUpdateTranslator.
 */
class InventoryItemUpdateTranslatorTest extends TranslatorTestBase {
    
    private static final int INVENTORY_ITEM_UPDATE_OPCODE = 0x3040;
    private InventoryItemUpdateTranslator translator;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        translator = new InventoryItemUpdateTranslator(createNullLookup());
    }
    
    @Test
    @DisplayName("Should parse inventory item update with item ID")
    void testItemUpdateWithId() {
        // Packet: slot (byte) + flags (byte) + itemId (int) [if FLAG_REF_OBJ_ID set]
        byte slot = 5;
        byte flags = InventoryItemUpdateEvent.FLAG_REF_OBJ_ID;
        int itemId = 50001;
        
        var packet = packetBuilder()
            .putByte(slot)
            .putByte(flags)
            .putInt(itemId)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        InventoryItemUpdateEvent event = assertSingleEvent(events, InventoryItemUpdateEvent.class);
        assertEquals(slot, event.getSlot());
        assertEquals(flags, event.getUpdateFlags());
        assertNotNull(event.getItemId());
        assertEquals(itemId, event.getItemId().intValue());
    }
    
    @Test
    @DisplayName("Should parse inventory item update with opt level")
    void testItemUpdateWithOptLevel() {
        byte slot = 10;
        byte flags = InventoryItemUpdateEvent.FLAG_OPT_LEVEL;
        byte optLevel = 5;
        
        var packet = packetBuilder()
            .putByte(slot)
            .putByte(flags)
            .putByte(optLevel)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        InventoryItemUpdateEvent event = assertSingleEvent(events, InventoryItemUpdateEvent.class);
        assertEquals(slot, event.getSlot());
        assertNotNull(event.getOptLevel());
        assertEquals(optLevel, event.getOptLevel().byteValue());
    }
    
    @Test
    @DisplayName("Should parse inventory item update with quantity")
    void testItemUpdateWithQuantity() {
        byte slot = 15;
        byte flags = InventoryItemUpdateEvent.FLAG_QUANTITY;
        int quantity = 50;
        
        var packet = packetBuilder()
            .putByte(slot)
            .putByte(flags)
            .putShort((short) quantity)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        InventoryItemUpdateEvent event = assertSingleEvent(events, InventoryItemUpdateEvent.class);
        assertEquals(slot, event.getSlot());
        assertNotNull(event.getQuantity());
        assertEquals(quantity, event.getQuantity().intValue());
    }
    
    @Test
    @DisplayName("Should parse inventory item update with durability")
    void testItemUpdateWithDurability() {
        byte slot = 20;
        byte flags = InventoryItemUpdateEvent.FLAG_DURABILITY;
        int durability = 8500;
        
        var packet = packetBuilder()
            .putByte(slot)
            .putByte(flags)
            .putInt(durability)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        InventoryItemUpdateEvent event = assertSingleEvent(events, InventoryItemUpdateEvent.class);
        assertEquals(slot, event.getSlot());
        assertNotNull(event.getDurability());
        assertEquals(durability, event.getDurability().intValue());
    }
    
    @Test
    @DisplayName("Should parse inventory item update with multiple flags")
    void testItemUpdateWithMultipleFlags() {
        byte slot = 25;
        byte flags = (byte) (InventoryItemUpdateEvent.FLAG_REF_OBJ_ID | 
                            InventoryItemUpdateEvent.FLAG_OPT_LEVEL | 
                            InventoryItemUpdateEvent.FLAG_QUANTITY);
        int itemId = 60001;
        byte optLevel = 3;
        int quantity = 10;
        
        var packet = packetBuilder()
            .putByte(slot)
            .putByte(flags)
            .putInt(itemId)
            .putByte(optLevel)
            .putShort((short) quantity)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        InventoryItemUpdateEvent event = assertSingleEvent(events, InventoryItemUpdateEvent.class);
        assertEquals(slot, event.getSlot());
        assertEquals(flags, event.getUpdateFlags());
        assertNotNull(event.getItemId());
        assertEquals(itemId, event.getItemId().intValue());
        assertNotNull(event.getOptLevel());
        assertEquals(optLevel, event.getOptLevel().byteValue());
        assertNotNull(event.getQuantity());
        assertEquals(quantity, event.getQuantity().intValue());
    }
    
    @Test
    @DisplayName("Should handle update with no flags")
    void testItemUpdateNoFlags() {
        byte slot = 30;
        byte flags = 0;
        
        var packet = packetBuilder()
            .putByte(slot)
            .putByte(flags)
            .build();
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        InventoryItemUpdateEvent event = assertSingleEvent(events, InventoryItemUpdateEvent.class);
        assertEquals(slot, event.getSlot());
        assertEquals(flags, event.getUpdateFlags());
        assertNull(event.getItemId());
        assertNull(event.getOptLevel());
        assertNull(event.getQuantity());
        assertNull(event.getDurability());
    }
    
    @Test
    @DisplayName("Should return empty list on malformed packet")
    void testMalformedPacket() {
        var packet = createPacket(new byte[] { 0x01 }); // Too short
        
        List<IGameEvent> events = translator.translate(TEST_MACHINE_NAME, packet, null);
        
        assertNoEvents(events);
    }
    
    @Test
    @DisplayName("Should have correct opcode")
    void testGetOpcode() {
        assertEquals(INVENTORY_ITEM_UPDATE_OPCODE, translator.getOpcode());
    }
}
