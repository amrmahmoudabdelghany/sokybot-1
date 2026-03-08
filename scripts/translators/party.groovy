import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.events.party.*

translator(0x3864) { machine, packet ->
    try {
        def r = packet.streamReader
        byte updateType = r.getByte()
        Integer memberUniqueId = null
        String memberName = null
        Byte memberLevel = null
        Byte memberHealthMana = null
        if (updateType == PartyUpdateEvent.TYPE_JOINED) {
            memberUniqueId = r.getInt()
            memberName = r.getString()
            memberLevel = r.getByte()
        } else if (updateType == PartyUpdateEvent.TYPE_LEAVE || updateType == PartyUpdateEvent.TYPE_MEMBER) {
            byte memberUpdateType = r.getByte()
            if (memberUpdateType == 0x04) memberHealthMana = r.getByte()
            else if (memberUpdateType == 0x08) memberLevel = r.getByte()
        }
        return singleEvent(new PartyUpdateEvent(machine, updateType, memberUniqueId, memberName, memberLevel, memberHealthMana))
    } catch (Exception e) { return noEvents() }
}

translator(0x3080) { machine, packet ->
    try {
        return singleEvent(new PartyInviteEvent(machine, packet.streamReader.getByte()))
    } catch (Exception e) { return noEvents() }
}

translator(0x306E) { machine, packet ->
    try {
        def r = packet.streamReader
        int partyIdJoin = r.getInt()
        int nameLen = r.getShort() & 0xFFFF
        String playerName = r.getUnicodeString(nameLen)
        return singleEvent(PartyMatchingEvent.joinRequest(machine, partyIdJoin, playerName))
    } catch (Exception e) { return noEvents() }
}

translator(0x3065) { machine, packet ->
    try {
        return singleEvent(PartyMatchingEvent.partyCreated(machine, packet.streamReader.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0xB067) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(PartyMatchingEvent.memberCountUpdate(machine, r.getInt(), r.getByte() & 0xFF))
    } catch (Exception e) { return noEvents() }
}

translator(0xB06C) { machine, packet ->
    try {
        def r = packet.streamReader
        byte result = r.getByte()
        byte pageCount = 0, pageIndex = 0
        def entries = []
        if (result == 1) {
            pageCount = r.getByte()
            pageIndex = r.getByte()
            byte partyCount = r.getByte()
            partyCount.times {
                entries << PartyMatchingListEvent.PartyMatchEntry.builder()
                    .partyNumber(r.getInt()).masterJid(r.getInt()).masterName(r.getString())
                    .countryType(r.getByte()).memberCount(r.getByte()).settingsFlag(r.getByte())
                    .purposeType(r.getByte()).levelMin(r.getByte()).levelMax(r.getByte()).title(r.getString()).build()
            }
        }
        return singleEvent(PartyMatchingListEvent.builder().fullName(machine).timestamp(System.currentTimeMillis()).result(result).pageCount(pageCount).pageIndex(pageIndex).entries(entries).build())
    } catch (Exception e) { return noEvents() }
}
