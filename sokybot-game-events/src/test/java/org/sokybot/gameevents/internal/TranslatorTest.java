package org.sokybot.gameevents.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.api.events.*;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Unit tests for new packet translators.
 * Uses mock packets created from byte arrays.
 */
class TranslatorTest {
    
    private IGameDataLookup mockLookup;
    private static final String MACHINE_NAME = "test-machine";
    
    @BeforeEach
    void setUp() {
        // Null lookup is safe - translators handle null gracefully
        mockLookup = null;
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Create a mock packet from a byte array.
     */
    private ImmutablePacket createPacket(byte[] data) {
        return ImmutablePacket.wrap(data);
    }
    
    /**
     * Create a ByteBuffer for building packets (little-endian like Silkroad).
     */
    private ByteBuffer newPacketBuilder(int capacity) {
        return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }
    
    // ========== EmotionTranslator Tests ==========
    
    @Test
    @DisplayName("EmotionTranslator: should parse emotion packet correctly")
    void testEmotionTranslator() {
        // Build mock packet: entityId (int) + emotionId (byte)
        ByteBuffer bb = newPacketBuilder(5);
        bb.putInt(12345);  // entityId
        bb.put((byte) 7);  // emotionId (e.g., wave)
        
        EmotionTranslator translator = new EmotionTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        EmotionEvent event = (EmotionEvent) events.get(0);
        assertEquals(12345, event.getEntityId());
        assertEquals(7, event.getEmotionId());
        assertEquals(MACHINE_NAME, event.getFullName());
    }
    
    // ========== PickupAnimationTranslator Tests ==========
    
    @Test
    @DisplayName("PickupAnimationTranslator: should parse pickup packet correctly")
    void testPickupAnimationTranslator() {
        // Build mock packet: entityId (int) + targetItemId (int)
        ByteBuffer bb = newPacketBuilder(8);
        bb.putInt(100);  // entityId
        bb.putInt(999);  // targetItemId
        
        PickupAnimationTranslator translator = new PickupAnimationTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        PickupAnimationEvent event = (PickupAnimationEvent) events.get(0);
        assertEquals(100, event.getEntityId());
        assertEquals(999, event.getTargetItemId());
    }
    
    // ========== EquipItemTranslator Tests ==========
    
    @Test
    @DisplayName("EquipItemTranslator: should parse equip item packet correctly")
    void testEquipItemTranslator() {
        // Build mock packet: entityId (int) + slot (byte) + itemRefId (int) + optLevel (byte)
        ByteBuffer bb = newPacketBuilder(10);
        bb.putInt(200);    // entityId
        bb.put((byte) 3);  // slot (weapon)
        bb.putInt(50001);  // itemRefId
        bb.put((byte) 5);  // optLevel (+5)
        
        EquipItemTranslator translator = new EquipItemTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        EquipItemVisualEvent event = (EquipItemVisualEvent) events.get(0);
        assertEquals(200, event.getEntityId());
        assertEquals(3, event.getSlot());
        assertEquals(50001, event.getItemRefId());
        assertEquals(5, event.getOptLevel());
    }
    
    // ========== UnequipItemTranslator Tests ==========
    
    @Test
    @DisplayName("UnequipItemTranslator: should parse unequip item packet correctly")
    void testUnequipItemTranslator() {
        // Build mock packet: entityId (int) + slot (byte)
        ByteBuffer bb = newPacketBuilder(5);
        bb.putInt(300);    // entityId
        bb.put((byte) 6);  // slot (chest)
        
        UnequipItemTranslator translator = new UnequipItemTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        UnequipItemVisualEvent event = (UnequipItemVisualEvent) events.get(0);
        assertEquals(300, event.getEntityId());
        assertEquals(6, event.getSlot());
    }
    
    // ========== DamageEffectTranslator Tests ==========
    
    @Test
    @DisplayName("DamageEffectTranslator: should parse damage effect packet correctly")
    void testDamageEffectTranslator() {
        // Build mock packet: targetId (int) + damageType (byte) + damageAmount (int)
        ByteBuffer bb = newPacketBuilder(9);
        bb.putInt(400);    // targetEntityId
        bb.put((byte) 1);  // damageType (critical)
        bb.putInt(2500);   // damageAmount
        
        DamageEffectTranslator translator = new DamageEffectTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        DamageEffectEvent event = (DamageEffectEvent) events.get(0);
        assertEquals(400, event.getTargetEntityId());
        assertEquals(2500, event.getDamageAmount());
        assertEquals(DamageEffectEvent.DamageType.CRITICAL, event.getDamageType());
    }
    
    @Test
    @DisplayName("DamageEffectTranslator: should handle normal damage type")
    void testDamageEffectTranslatorNormal() {
        ByteBuffer bb = newPacketBuilder(9);
        bb.putInt(500);
        bb.put((byte) 0);  // NORMAL
        bb.putInt(100);
        
        DamageEffectTranslator translator = new DamageEffectTranslator(mockLookup);
        DamageEffectEvent event = (DamageEffectEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        assertEquals(DamageEffectEvent.DamageType.NORMAL, event.getDamageType());
    }
    
    // ========== StatPointsUpdateTranslator Tests ==========
    
    @Test
    @DisplayName("StatPointsUpdateTranslator: should parse STR update correctly")
    void testStatPointsUpdateTranslatorStr() {
        // Build mock packet: result (byte) + remainingPoints (short)
        ByteBuffer bb = newPacketBuilder(3);
        bb.put((byte) 1);     // success
        bb.putShort((short) 50);  // remainingPoints
        
        StatPointsUpdateTranslator translator = StatPointsUpdateTranslator.forStrength(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        StatPointsUpdateEvent event = (StatPointsUpdateEvent) events.get(0);
        assertEquals(StatPointsUpdateEvent.StatType.STRENGTH, event.getStatType());
        assertTrue(event.isSuccess());
        assertEquals(50, event.getRemainingPoints());
    }
    
    @Test
    @DisplayName("StatPointsUpdateTranslator: should handle failed allocation")
    void testStatPointsUpdateTranslatorFailed() {
        ByteBuffer bb = newPacketBuilder(1);
        bb.put((byte) 0);  // failed
        
        StatPointsUpdateTranslator translator = StatPointsUpdateTranslator.forIntelligence(mockLookup);
        StatPointsUpdateEvent event = (StatPointsUpdateEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertFalse(event.isSuccess());
        assertEquals(StatPointsUpdateEvent.StatType.INTELLIGENCE, event.getStatType());
    }
    
    // ========== HwanLevelUpdateTranslator Tests ==========
    
    @Test
    @DisplayName("HwanLevelUpdateTranslator: should parse Hwan update correctly")
    void testHwanLevelUpdateTranslator() {
        // Build mock packet: entityId (int) + hwanLevel (byte) + hwanProgress (int)
        ByteBuffer bb = newPacketBuilder(9);
        bb.putInt(600);    // entityId
        bb.put((byte) 3);  // hwanLevel
        bb.putInt(75000);  // hwanProgress
        
        HwanLevelUpdateTranslator translator = new HwanLevelUpdateTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        HwanLevelUpdateEvent event = (HwanLevelUpdateEvent) events.get(0);
        assertEquals(600, event.getEntityId());
        assertEquals(3, event.getHwanLevel());
        assertEquals(75000, event.getHwanProgress());
    }
    
    // ========== AttackSpeedUpdateTranslator Tests ==========
    
    @Test
    @DisplayName("AttackSpeedUpdateTranslator: should parse attack speed correctly")
    void testAttackSpeedUpdateTranslator() {
        // Build mock packet: entityId (int) + attackSpeed (int)
        ByteBuffer bb = newPacketBuilder(8);
        bb.putInt(700);   // entityId
        bb.putInt(150);   // attackSpeed
        
        AttackSpeedUpdateTranslator translator = new AttackSpeedUpdateTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(bb.array()));
        
        assertEquals(1, events.size());
        AttackSpeedUpdateEvent event = (AttackSpeedUpdateEvent) events.get(0);
        assertEquals(700, event.getEntityId());
        assertEquals(150, event.getAttackSpeed());
    }
    
    // ========== ExchangeConfirmedTranslator Tests ==========
    
    @Test
    @DisplayName("ExchangeConfirmedTranslator: should parse self confirmed correctly")
    void testExchangeConfirmedTranslatorSelf() {
        // Build mock packet: exchangeId (int) + confirmedBy (byte)
        ByteBuffer bb = newPacketBuilder(5);
        bb.putInt(1001);   // exchangeId
        bb.put((byte) 0);  // self confirmed
        
        ExchangeConfirmedTranslator translator = new ExchangeConfirmedTranslator(mockLookup);
        ExchangeConfirmedEvent event = (ExchangeConfirmedEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(1001, event.getExchangeId());
        assertTrue(event.isSelfConfirmed());
    }
    
    @Test
    @DisplayName("ExchangeConfirmedTranslator: should parse partner confirmed correctly")
    void testExchangeConfirmedTranslatorPartner() {
        ByteBuffer bb = newPacketBuilder(5);
        bb.putInt(1002);
        bb.put((byte) 1);  // partner confirmed
        
        ExchangeConfirmedTranslator translator = new ExchangeConfirmedTranslator(mockLookup);
        ExchangeConfirmedEvent event = (ExchangeConfirmedEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertFalse(event.isSelfConfirmed());
    }
    
    // ========== ExchangeApprovedTranslator Tests ==========
    
    @Test
    @DisplayName("ExchangeApprovedTranslator: should parse approved correctly")
    void testExchangeApprovedTranslator() {
        ByteBuffer bb = newPacketBuilder(5);
        bb.putInt(2001);   // exchangeId
        bb.put((byte) 1);  // success
        
        ExchangeApprovedTranslator translator = new ExchangeApprovedTranslator(mockLookup);
        ExchangeApprovedEvent event = (ExchangeApprovedEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(2001, event.getExchangeId());
        assertTrue(event.isSuccess());
    }
    
    // ========== ExchangeUpdateTranslator Tests ==========
    
    @Test
    @DisplayName("ExchangeUpdateTranslator: should parse exchange update correctly")
    void testExchangeUpdateTranslator() {
        // Build mock packet: exchangeId + updatedBy + slot + itemRefId + quantity + goldAmount
        ByteBuffer bb = newPacketBuilder(20);
        bb.putInt(3001);      // exchangeId
        bb.put((byte) 0);     // self update
        bb.put((byte) 2);     // slot
        bb.putInt(60001);     // itemRefId
        bb.putShort((short) 5);  // quantity
        bb.putLong(100000L);  // goldAmount
        
        ExchangeUpdateTranslator translator = new ExchangeUpdateTranslator(mockLookup);
        ExchangeUpdateEvent event = (ExchangeUpdateEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(3001, event.getExchangeId());
        assertTrue(event.isSelfUpdate());
        assertEquals(2, event.getSlot());
        assertEquals(60001, event.getItemRefId());
        assertEquals(5, event.getQuantity());
        assertEquals(100000L, event.getGoldAmount());
    }
    
    // ========== StallEventTranslator Tests ==========
    
    @Test
    @DisplayName("StallEventTranslator: should parse stall created correctly")
    void testStallEventTranslatorCreated() {
        ByteBuffer bb = newPacketBuilder(4);
        bb.putInt(5001);  // entityId
        
        StallEventTranslator translator = StallEventTranslator.forCreated(mockLookup);
        StallEvent event = (StallEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(5001, event.getEntityId());
        assertEquals(StallEvent.StallEventType.CREATED, event.getEventType());
    }
    
    @Test
    @DisplayName("StallEventTranslator: should parse stall destroyed correctly")
    void testStallEventTranslatorDestroyed() {
        ByteBuffer bb = newPacketBuilder(4);
        bb.putInt(5002);
        
        StallEventTranslator translator = StallEventTranslator.forDestroyed(mockLookup);
        StallEvent event = (StallEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(StallEvent.StallEventType.DESTROYED, event.getEventType());
    }
    
    // ========== PartyMatchingTranslator Tests ==========
    
    @Test
    @DisplayName("PartyMatchingTranslator: should parse party created correctly")
    void testPartyMatchingTranslatorCreated() {
        ByteBuffer bb = newPacketBuilder(4);
        bb.putInt(9001);  // partyId
        
        PartyMatchingTranslator translator = PartyMatchingTranslator.forPartyCreated(mockLookup);
        PartyMatchingEvent event = (PartyMatchingEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(9001, event.getPartyId());
        assertEquals(PartyMatchingEvent.MatchingEventType.PARTY_CREATED, event.getEventType());
    }
    
    @Test
    @DisplayName("PartyMatchingTranslator: should parse member count update correctly")
    void testPartyMatchingTranslatorMemberCount() {
        ByteBuffer bb = newPacketBuilder(5);
        bb.putInt(9002);   // partyId
        bb.put((byte) 4);  // memberCount
        
        PartyMatchingTranslator translator = PartyMatchingTranslator.forMemberCountUpdate(mockLookup);
        PartyMatchingEvent event = (PartyMatchingEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(9002, event.getPartyId());
        assertEquals(4, event.getMemberCount());
        assertEquals(PartyMatchingEvent.MatchingEventType.MEMBER_COUNT_UPDATE, event.getEventType());
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    @DisplayName("Translators should return empty list on malformed packets")
    void testTranslatorMalformedPacket() {
        // Empty packet should not crash translators
        byte[] emptyData = new byte[0];
        
        EmotionTranslator emotionTranslator = new EmotionTranslator(mockLookup);
        assertTrue(emotionTranslator.translate(MACHINE_NAME, createPacket(emptyData)).isEmpty());
        
        EquipItemTranslator equipTranslator = new EquipItemTranslator(mockLookup);
        assertTrue(equipTranslator.translate(MACHINE_NAME, createPacket(emptyData)).isEmpty());
        
        DamageEffectTranslator damageTranslator = new DamageEffectTranslator(mockLookup);
        assertTrue(damageTranslator.translate(MACHINE_NAME, createPacket(emptyData)).isEmpty());
    }
    
    @Test
    @DisplayName("Translators should handle truncated packets gracefully")
    void testTranslatorTruncatedPacket() {
        // Packet too short (only 2 bytes when 4+ expected)
        byte[] truncatedData = new byte[] { 0x01, 0x02 };
        
        EquipItemTranslator translator = new EquipItemTranslator(mockLookup);
        List<IGameEvent> events = translator.translate(MACHINE_NAME, createPacket(truncatedData));
        
        assertTrue(events.isEmpty(), "Translator should return empty on truncated packet");
    }
    
    // ========== Phase 2 Translator Tests ==========
    
    @Test
    @DisplayName("LevelUpTranslator: should parse level up correctly")
    void testLevelUpTranslator() {
        ByteBuffer bb = newPacketBuilder(4);
        bb.putInt(7001);  // entityId
        
        LevelUpTranslator translator = new LevelUpTranslator(mockLookup);
        LevelUpEvent event = (LevelUpEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(7001, event.getEntityId());
    }
    
    @Test
    @DisplayName("ItemOwnershipRemovedTranslator: should parse correctly")
    void testItemOwnershipRemovedTranslator() {
        ByteBuffer bb = newPacketBuilder(4);
        bb.putInt(8001);  // itemUniqueId
        
        ItemOwnershipRemovedTranslator translator = new ItemOwnershipRemovedTranslator(mockLookup);
        ItemOwnershipRemovedEvent event = (ItemOwnershipRemovedEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(8001, event.getItemUniqueId());
    }
    
    @Test
    @DisplayName("QuestAbandonTranslator: should parse success correctly")
    void testQuestAbandonTranslator() {
        ByteBuffer bb = newPacketBuilder(5);
        bb.put((byte) 1);  // success
        bb.putInt(5001);   // questId
        
        QuestAbandonTranslator translator = new QuestAbandonTranslator(mockLookup);
        QuestAbandonEvent event = (QuestAbandonEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertTrue(event.isSuccess());
        assertEquals(5001, event.getQuestId());
    }
    
    @Test
    @DisplayName("CelestialPositionTranslator: should parse correctly")
    void testCelestialPositionTranslator() {
        ByteBuffer bb = newPacketBuilder(6);
        bb.putInt(1);      // uniqueId
        bb.putShort((short) 180);  // angle
        
        CelestialPositionTranslator translator = new CelestialPositionTranslator(mockLookup);
        CelestialPositionEvent event = (CelestialPositionEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(1, event.getUniqueId());
        assertEquals(180, event.getAngle());
    }
    
    @Test
    @DisplayName("WeatherUpdateTranslator: should parse rain correctly")
    void testWeatherUpdateTranslator() {
        ByteBuffer bb = newPacketBuilder(2);
        bb.put((byte) 2);  // rain
        bb.put((byte) 50); // intensity
        
        WeatherUpdateTranslator translator = new WeatherUpdateTranslator(mockLookup);
        WeatherUpdateEvent event = (WeatherUpdateEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(WeatherUpdateEvent.WeatherType.RAIN, event.getWeatherType());
        assertEquals(50, event.getIntensity());
    }
    
    @Test
    @DisplayName("ChatRestrictTranslator: should parse correctly")
    void testChatRestrictTranslator() {
        ByteBuffer bb = newPacketBuilder(4);
        bb.putInt(60);  // 60 seconds
        
        ChatRestrictTranslator translator = new ChatRestrictTranslator(mockLookup);
        ChatRestrictEvent event = (ChatRestrictEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertEquals(60, event.getDurationSeconds());
    }
    
    @Test
    @DisplayName("ItemRepairTranslator: should parse success correctly")
    void testItemRepairTranslator() {
        ByteBuffer bb = newPacketBuilder(10);
        bb.put((byte) 1);   // success
        bb.put((byte) 5);   // slot
        bb.putLong(50000L); // cost
        
        ItemRepairTranslator translator = new ItemRepairTranslator(mockLookup);
        ItemRepairEvent event = (ItemRepairEvent) translator.translate(MACHINE_NAME, createPacket(bb.array())).get(0);
        
        assertTrue(event.isSuccess());
        assertEquals(5, event.getSlot());
        assertEquals(50000L, event.getCost());
    }
}
