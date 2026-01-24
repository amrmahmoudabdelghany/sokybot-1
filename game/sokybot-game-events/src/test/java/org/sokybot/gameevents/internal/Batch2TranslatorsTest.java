package org.sokybot.gameevents.internal;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.academy.AcademyUpdateEvent;
import org.sokybot.gameevents.events.alchemy.AlchemyDismantleEvent;
import org.sokybot.gameevents.events.consignment.ConsignmentEvent;
import org.sokybot.gameevents.events.consignment.ConsignmentEvent.ConsignmentEventType;
import org.sokybot.gameevents.events.world.GameNotifyEvent;
import org.sokybot.gameevents.events.world.GameNotifyEvent.NotifyType;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IPacketBuilder;
import org.sokybot.network.packet.MutablePacket;

import static org.junit.jupiter.api.Assertions.*;

class Batch2TranslatorsTest {

    @Test
    void testConsignmentTranslatorList() {
        ConsignmentTranslator translator = new ConsignmentTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(128, 0xB50E);
        builder.put((byte) 1); // result: SUCCESS
        builder.put((byte) 1); // itemCount
        builder.putInt(12345); // personalId
        builder.put((byte) 1); // saleStatus
        builder.putInt(99); // refItemId
        builder.putInt(5); // sellCount
        builder.putLong(1000L); // price
        builder.putLong(100L); // deposit
        builder.putLong(10L); // sellFee
        builder.putInt(1674550000); // endDate

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        ConsignmentEvent event = (ConsignmentEvent) events.get(0);
        assertEquals(ConsignmentEventType.LIST, event.getType());
        assertEquals(1, event.getItems().size());
        assertEquals(12345, event.getItems().get(0).getPersonalId());
        assertEquals("Legion.M1", event.getFullName());
    }

    @Test
    void testAcademyUpdateTranslator() {
        AcademyUpdateTranslator translator = new AcademyUpdateTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(32, 0x3C80);
        builder.put((byte) 5); // update_type: HONOR_POINT
        builder.putInt(999); // charId
        builder.put((byte) 0);
        builder.put((byte) 0); // unk
        builder.put((byte) 4); // balance length
        builder.putInt(5000); // NewHonorBalance

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        AcademyUpdateEvent event = (AcademyUpdateEvent) events.get(0);
        assertEquals(5000, event.getNewHonorBalance());
        assertEquals("Legion.M1", event.getFullName());
    }

    @Test
    void testGameNotifyTranslatorUniqueSpawn() {
        GameNotifyTranslator translator = new GameNotifyTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(32, 0x300C);
        builder.put((byte) 0x05); // result: UNIQUE_SPAWNED
        builder.put((byte) 1); // internal result
        builder.putInt(1933); // modelId (Tiger Girl)

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()), null);
        assertEquals(1, events.size());
        GameNotifyEvent event = (GameNotifyEvent) events.get(0);
        assertEquals(NotifyType.UNIQUE_SPAWNED, event.getType());
        assertEquals(1933, event.getModelId());
        assertEquals("Legion.M1", event.getFullName());
    }

    @Test
    void testAlchemyDismantleTranslator() {
        AlchemyDismantleTranslator translator = new AlchemyDismantleTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(16, 0xB157);
        builder.put((byte) 1); // result: SUCCESS

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()), null);
        assertEquals(1, events.size());
        AlchemyDismantleEvent event = (AlchemyDismantleEvent) events.get(0);
        assertEquals((byte) 1, event.getResult());
        assertEquals("Legion.M1", event.getFullName());
    }
}
