import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.events.chat.*
import org.sokybot.gameevents.events.community.FriendListInfoEvent
import org.sokybot.gameevents.events.guild.*

translator(0x3026) { machine, packet ->
    try {
        def r = packet.streamReader
        byte v = r.getByte()
        def chatType = (v == 1) ? ChatMessageEvent.ChatType.ALL : (v == 2) ? ChatMessageEvent.ChatType.PRIVATE : (v == 3) ? ChatMessageEvent.ChatType.PARTY : (v == 4) ? ChatMessageEvent.ChatType.GUILD : (v == 5) ? ChatMessageEvent.ChatType.GLOBAL : (v == 6) ? ChatMessageEvent.ChatType.NOTICE : (v == 7) ? ChatMessageEvent.ChatType.STALL : (v == 9) ? ChatMessageEvent.ChatType.UNION : (v == 11) ? ChatMessageEvent.ChatType.ACADEMY : ChatMessageEvent.ChatType.UNKNOWN
        String senderName = PacketReaderUtils.readString(r)
        String message = PacketReaderUtils.readString(r)
        return singleEvent(new ChatMessageEvent(machine, chatType, senderName, message))
    } catch (Exception e) { return noEvents() }
}

translator(0x302D) { machine, packet ->
    try {
        return singleEvent(new ChatRestrictEvent(machine, packet.streamReader.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3091) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new EmotionEvent(machine, r.getInt(), r.getUnsignedByte()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3305) { machine, packet ->
    try {
        def r = packet.streamReader
        int groupCount = r.getByte() & 0xFF
        def groups = (0..<groupCount).collect {
            FriendListInfoEvent.FriendGroup.builder().id(r.getShort() & 0xFFFF).name(r.getString()).build()
        }
        int friendCount = r.getByte() & 0xFF
        def friends = (0..<friendCount).collect {
            FriendListInfoEvent.FriendEntry.builder()
                .charId(r.getInt()).name(r.getString()).modelId(r.getInt())
                .groupId(r.getShort() & 0xFFFF).offline(r.getByte() != 0).build()
        }
        return singleEvent(new FriendListInfoEvent(machine, groups, friends))
    } catch (Exception e) { return noEvents() }
}

translator(0x3101) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new GuildInfoEvent(machine, r.getInt(), r.getString(), r.getUnsignedByte(), r.getUnsignedByte()))
    } catch (Exception e) { return noEvents() }
}

translator(0x30FF) { machine, packet ->
    try {
        def r = packet.streamReader
        def builder = GuildEntityUpdateEvent.builder().fullName(machine).dwGid(r.getInt()).guildId(r.getInt())
        builder.guildName(PacketReaderUtils.readString(r))
        builder.grandName(PacketReaderUtils.readString(r))
        builder.guildCrestRev(r.getInt()).unionId(r.getInt()).unionCrestRev(r.getInt()).isFriendly(r.getByte()).siegeAuthority(r.getByte())
        return singleEvent(builder.build())
    } catch (Exception e) { return noEvents() }
}
