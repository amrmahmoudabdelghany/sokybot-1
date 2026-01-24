package org.sokybot.gameevents.internal;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.character.CharacterJoinEvent;
import org.sokybot.gameevents.events.world.FRPVPUpdateEvent;
import org.sokybot.gameevents.events.storage.StorageDataEvent;
import org.sokybot.gameevents.events.tap.TapUpdateEvent;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IPacketBuilder;
import org.sokybot.network.packet.MutablePacket;

import static org.junit.jupiter.api.Assertions.*;

class Batch4TranslatorsTest {

    @Test
    void testCharacterJoinTranslatorSuccess() {
        CharacterJoinTranslator translator = new CharacterJoinTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(16, 0xB001);
        builder.put((byte) 1); // result: SUCCESS

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()), null);
        assertEquals(1, events.size());
        CharacterJoinEvent event = (CharacterJoinEvent) events.get(0);
        assertTrue(event.isSuccess());
        assertNull(event.getErrorCode());
    }

    @Test
    void testFRPVPUpdateTranslator() {
        FRPVPUpdateTranslator translator = new FRPVPUpdateTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(16, 0xB516);
        builder.put((byte) 1); // result: SUCCESS
        builder.putInt(12345); // uniqueId
        builder.put((byte) 1); // mode

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()), null);
        assertEquals(1, events.size());
        FRPVPUpdateEvent event = (FRPVPUpdateEvent) events.get(0);
        assertEquals(12345, event.getUniqueId());
        assertEquals((byte) 1, event.getMode());
    }

    @Test
    void testStorageDataTranslator() {
        StorageDataTranslator translator = new StorageDataTranslator();
        byte[] payload = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        IPacketBuilder builder = MutablePacket.getBuilder(payload.length, 0x3049);
        builder.putBytes(payload);

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        StorageDataEvent event = (StorageDataEvent) events.get(0);
        assertArrayEquals(payload, event.getData());
    }

    @Test
    void testTapUpdateTranslator() {
        TapUpdateTranslator translator = new TapUpdateTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(16, 0xB4E0);
        builder.putInt(200); // trader points
        builder.putInt(100); // thief points
        builder.putInt(300); // trader points
        builder.putInt(150); // thief points

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        TapUpdateEvent event = (TapUpdateEvent) events.get(0);
        assertEquals(2, event.getEntries().size());
        assertEquals(200, event.getEntries().get(0).getTraderUnionPoints());
        assertEquals(150, event.getEntries().get(1).getThiefUnionPoints());
    }
}
