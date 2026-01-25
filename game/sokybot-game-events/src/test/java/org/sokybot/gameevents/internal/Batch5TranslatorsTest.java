package org.sokybot.gameevents.internal;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.character.CharacterSelectionActionEvent;
import org.sokybot.gameevents.events.consignment.ConsignmentDetailEvent;
import org.sokybot.gameevents.events.consignment.ConsignmentSearchEvent;
import org.sokybot.gameevents.events.party.PartyMatchingListEvent;
import org.sokybot.gameevents.events.world.SiegeUpdateEvent;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IPacketBuilder;
import org.sokybot.network.packet.MutablePacket;

import static org.junit.jupiter.api.Assertions.*;

class Batch5TranslatorsTest {

    @Test
    void testPartyMatchingListTranslator() {
        PartyMatchingListTranslator translator = new PartyMatchingListTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(128, 0xB06C);
        builder.put((byte) 1); // result: SUCCESS
        builder.put((byte) 5); // pageCount
        builder.put((byte) 1); // pageIndex
        builder.put((byte) 1); // partyCount
        builder.putInt(1001); // partyNumber
        builder.putInt(555); // masterJid
        putString(builder, "SokyMaster"); // masterName
        builder.put((byte) 1); // countryType
        builder.put((byte) 4); // memberCount
        builder.put((byte) 0); // settingsFlag
        builder.put((byte) 0); // purposeType
        builder.put((byte) 1); // levelMin
        builder.put((byte) 140); // levelMax
        putString(builder, "Lvl 140 Party"); // title

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        PartyMatchingListEvent event = (PartyMatchingListEvent) events.get(0);
        assertEquals(1, event.getEntries().size());
        assertEquals("SokyMaster", event.getEntries().get(0).getMasterName());
        assertEquals("Lvl 140 Party", event.getEntries().get(0).getTitle());
    }

    @Test
    void testCharacterSelectionActionTranslatorList() {
        CharacterSelectionActionTranslator translator = new CharacterSelectionActionTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(512, 0xB007);
        builder.put((byte) 1); // action (List)
        builder.put((byte) 1); // result: SUCCESS
        builder.put((byte) 1); // characterCount
        builder.putInt(1933); // refObjId
        putString(builder, "SokyChar"); // name
        builder.put((byte) 0); // scale
        builder.put((byte) 100); // level
        builder.putLong(1000000L); // expOffset
        builder.putShort((short) 200); // str
        builder.putShort((short) 200); // intell
        builder.putShort((short) 10); // statPoints
        builder.putInt(5000); // curHP
        builder.putInt(5000); // curMP
        builder.put((byte) 0); // isDeleting: FALSE
        builder.put((byte) 5); // guildMemberClass
        builder.put((byte) 0); // isGuildRenameRequired: FALSE
        builder.put((byte) 0); // academyMemberClass
        builder.put((byte) 1); // itemCount
        builder.putInt(101); // refItemId
        builder.put((byte) 5); // plus
        builder.put((byte) 0); // avatarItemCount

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        CharacterSelectionActionEvent event = (CharacterSelectionActionEvent) events.get(0);
        assertEquals(1, event.getCharacters().size());
        assertEquals("SokyChar", event.getCharacters().get(0).getName());
        assertEquals(101, event.getCharacters().get(0).getItems().get(0).getRefItemId());
    }

    @Test
    void testConsignmentSearchTranslator() {
        ConsignmentSearchTranslator translator = new ConsignmentSearchTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(128, 0xB50C);
        builder.put((byte) 1); // result: SUCCESS
        builder.put((byte) 1); // entryCount
        builder.put((byte) 10); // pageCount
        builder.putInt(999); // personalId
        putString(builder, "SellerOne"); // sellerName
        builder.put((byte) 1); // saleStatus
        builder.putInt(505); // refItemId
        builder.putInt(1); // sellCount
        builder.putLong(5000000L); // price
        builder.putInt(1674550000); // regDate

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        ConsignmentSearchEvent event = (ConsignmentSearchEvent) events.get(0);
        assertEquals(1, event.getEntries().size());
        assertEquals("SellerOne", event.getEntries().get(0).getSellerName());
        assertEquals(5000000L, event.getEntries().get(0).getPrice());
    }

    @Test
    void testConsignmentDetailTranslator() {
        ConsignmentDetailTranslator translator = new ConsignmentDetailTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(128, 0xB506);
        builder.put((byte) 1); // result: SUCCESS
        builder.put((byte) 1); // detailType
        putString(builder, "SellerOne"); // sellerName
        builder.putInt(999); // personalId
        builder.putInt(505); // refItemId
        builder.put((byte) 8); // plus
        builder.putLong(123456789L); // variance
        builder.putInt(1); // quantity
        builder.putInt(5000); // durability
        builder.put((byte) 1); // magicOptionCount
        builder.putInt(1); // type
        builder.putInt(100); // value

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        ConsignmentDetailEvent event = (ConsignmentDetailEvent) events.get(0);
        assertNotNull(event.getItem());
        assertEquals((byte) 8, event.getItem().getPlus());
        assertEquals(1, event.getItem().getMagicOptions().size());
    }

    @Test
    void testSiegeUpdateTranslatorInfo() {
        SiegeUpdateTranslator translator = new SiegeUpdateTranslator();
        IPacketBuilder builder = MutablePacket.getBuilder(1024, 0x385F);
        builder.put((byte) 0); // type: INFO
        builder.put((byte) 1); // fortressCount
        builder.putInt(1); // id (Jangan)
        putString(builder, "SokyGuild"); // guildName
        builder.putInt(101); // guildId
        putString(builder, "SokyLeader"); // leaderName
        putString(builder, "Attack!"); // instruction
        builder.putInt(1); // guildCrestRev
        builder.putInt(0); // unionId
        builder.putInt(0); // unionCrestRev
        builder.put((byte) 1); // enterCountdownEnabled
        builder.putInt(600); // enterCountdown
        builder.put((byte) 0); // stoneCooldownEnabled
        builder.put((byte) 2); // siegePeriod
        builder.putInt(0); // owningFortressID

        var events = translator.translate("Legion.M1", ImmutablePacket.wrap(builder.build().unwrap()));
        assertEquals(1, events.size());
        SiegeUpdateEvent event = (SiegeUpdateEvent) events.get(0);
        assertEquals(SiegeUpdateEvent.SiegeUpdateType.INFO, event.getType());
        assertEquals(1, event.getFortresses().size());
        assertEquals("SokyGuild", event.getFortresses().get(0).getGuildName());
    }

    private void putString(IPacketBuilder builder, String value) {
        if (value == null) {
            builder.putShort((short) 0);
        } else {
            byte[] bytes = value.getBytes();
            builder.putShort((short) bytes.length);
            builder.putBytes(bytes);
        }
    }
}
