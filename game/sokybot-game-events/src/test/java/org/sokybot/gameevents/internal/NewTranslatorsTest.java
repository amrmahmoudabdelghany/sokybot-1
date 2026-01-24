package org.sokybot.gameevents.internal;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.academy.AcademyMatchingListEvent;
import org.sokybot.gameevents.events.arena.BattleArenaOperationEvent;
import org.sokybot.gameevents.events.guild.GuildEntityUpdateEvent;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.packet.IPacketBuilder;

import static org.junit.jupiter.api.Assertions.*;

class NewTranslatorsTest {

    @Test
    void testAcademyMatchingListTranslator() {
        AcademyMatchingListTranslator translator = new AcademyMatchingListTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(1024, 0xB47D);
        builder.put((byte) 1); // result
        builder.put((byte) 5); // pageCount
        builder.put((byte) 2); // pageIndex
        builder.put((byte) 1); // matchCount

        builder.putInt(1234); // match number
        builder.putInt(0); // unkUInt01
        builder.put((byte) 1); // countryType
        builder.putShort((short) 4); // title len
        builder.putBytes("test".getBytes());
        builder.putInt(0);
        builder.putInt(0); // unkUInt02, unkUInt03
        builder.put((byte) 10); // displayLevel
        builder.put((byte) 50); // leaderLevel
        builder.putInt(999); // leaderId
        builder.putShort((short) 5); // name len
        builder.putBytes("AmrV8".getBytes());
        builder.putInt(10); // graduatedCount
        builder.put((byte) 1); // honorRank
        builder.putLong(0); // unkULong04

        var events = translator.translate("Group.Machine", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        AcademyMatchingListEvent event = (AcademyMatchingListEvent) events.get(0);
        assertEquals(1, event.getMatches().size());
        assertEquals("AmrV8", event.getMatches().get(0).getLeaderName());
    }

    @Test
    void testBattleArenaOperationTranslator() {
        BattleArenaOperationTranslator translator = new BattleArenaOperationTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(128, 0x34D2);
        builder.put((byte) 8); // operation: START
        builder.putInt(300000); // maxTime: 5 mins

        var events = translator.translate("Group.Machine", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        BattleArenaOperationEvent event = (BattleArenaOperationEvent) events.get(0);
        assertEquals((byte) 8, event.getOperation());
        assertEquals(300000, event.getData().get("maxTime"));
    }

    @Test
    void testGuildEntityUpdateTranslator() {
        GuildEntityUpdateTranslator translator = new GuildEntityUpdateTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(256, 0x30FF);
        builder.putInt(100); // dwGid
        builder.putInt(1); // guildId
        builder.putShort((short) 5);
        builder.putBytes("Guild".getBytes()); // guildName
        builder.putShort((short) 0); // grandName
        builder.putInt(0); // guildCrestRev
        builder.putInt(0); // unionId
        builder.putInt(0); // unionCrestRev
        builder.put((byte) 0); // isFriendly
        builder.put((byte) 0); // siegeAuthority

        var events = translator.translate("Group.Machine", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        GuildEntityUpdateEvent event = (GuildEntityUpdateEvent) events.get(0);
        assertEquals("Guild", event.getGuildName());
    }
}
