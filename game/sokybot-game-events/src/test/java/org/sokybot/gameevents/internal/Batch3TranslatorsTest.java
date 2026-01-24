package org.sokybot.gameevents.internal;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.pk.PKUpdateEvent;
import org.sokybot.gameevents.events.pk.PKUpdateEvent.PKUpdateType;
import org.sokybot.gameevents.events.stall.StallUpdateEvent;
import org.sokybot.gameevents.events.stall.StallUpdateEvent.StallUpdateType;
import org.sokybot.gameevents.events.tap.TapInfoEvent;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IPacketBuilder;
import org.sokybot.network.packet.MutablePacket;

import static org.junit.jupiter.api.Assertions.*;

class Batch3TranslatorsTest {

    @Test
    void testPKUpdateTranslatorPenalty() {
        PKUpdateTranslator translator = new PKUpdateTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(16, 0x30CD);
        builder.putInt(3600); // penaltyPoints

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()), null);
        assertEquals(1, events.size());
        PKUpdateEvent event = (PKUpdateEvent) events.get(0);
        assertEquals(PKUpdateType.PENALTY, event.getType());
        assertEquals(3600, event.getPenaltyPoints());
    }

    @Test
    void testStallUpdateTranslatorState() {
        StallUpdateTranslator translator = new StallUpdateTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(16, 0xB0BA);
        builder.put((byte) 1); // result: SUCCESS
        builder.put((byte) 5); // type: STATE
        builder.put((byte) 1); // isOpen: TRUE
        builder.putShort((short) 1); // errorCode: SUCCESS

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()), null);
        assertEquals(1, events.size());
        StallUpdateEvent event = (StallUpdateEvent) events.get(0);
        assertEquals(StallUpdateType.STATE, event.getType());
        assertTrue(event.getIsOpen());
    }

    @Test
    void testTapInfoTranslator() {
        TapInfoTranslator translator = new TapInfoTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(64, 0xB4DF);
        builder.put((byte) 1); // count
        builder.put((byte) 1); // entryType
        builder.put((byte) 1);
        builder.put((byte) 1);
        builder.put((byte) 12);
        builder.put((byte) 0); // from
        builder.put((byte) 1);
        builder.put((byte) 1);
        builder.put((byte) 14);
        builder.put((byte) 0); // to
        builder.putInt(100); // trader points
        builder.putInt(50); // thief points

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()), null);
        assertEquals(1, events.size());
        TapInfoEvent event = (TapInfoEvent) events.get(0);
        assertEquals(1, event.getEntries().size());
        assertEquals(100, event.getEntries().get(0).getTraderUnionPoints());
        assertEquals(50, event.getEntries().get(0).getThiefUnionPoints());
    }
}
