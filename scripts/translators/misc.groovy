import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.events.storage.*
import org.sokybot.gameevents.events.quest.*
import org.sokybot.gameevents.events.chat.NpcTalkEvent
import org.sokybot.gameevents.events.job.*
import org.sokybot.gameevents.events.alchemy.*
import org.sokybot.gameevents.events.teleport.*
import org.sokybot.gameevents.events.academy.*
import org.sokybot.gameevents.events.pk.*
import org.sokybot.gameevents.events.tap.*
import org.sokybot.gameevents.events.buff.DamageEffectEvent
import org.sokybot.gameevents.events.combat.BerserkConfirmEvent
import org.sokybot.gameevents.events.world.FRPVPUpdateEvent

// --- Storage ---
translator(0x3047) { machine, packet ->
    try {
        return singleEvent(new StorageOpenEvent(machine, packet.streamReader.getLong(), (byte)0))
    } catch (Exception e) { return noEvents() }
}

translator(0x3253) { machine, packet ->
    try {
        return singleEvent(new StorageOpenEvent(machine, packet.streamReader.getLong(), (byte)1))
    } catch (Exception e) { return noEvents() }
}

translator(0xB558) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = r.getBoolean()
        int itemCount = success ? r.getInt() : 0
        return singleEvent(new StorageBoxTakeItemEvent(machine, success, itemCount))
    } catch (Exception e) { return noEvents() }
}

translator(0x3049) { machine, packet ->
    try {
        return singleEvent(StorageDataEvent.builder().fullName(machine).timestamp(System.currentTimeMillis()).data(packet.packetReader.readFully()).build())
    } catch (Exception e) { return noEvents() }
}

// --- Quest ---
translator(0x30D5) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new QuestUpdateEvent(machine, r.getByte(), r.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0xB0D9) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = r.getByte() == 0x01
        int questId = success ? r.getInt() : 0
        return singleEvent(new QuestAbandonEvent(machine, success, questId))
    } catch (Exception e) { return noEvents() }
}

// --- NpcTalk ---
translator(0xB046) { machine, packet ->
    try {
        def r = packet.streamReader
        if (r.getByte() != 0x01) return noEvents()
        return singleEvent(new NpcTalkEvent(machine, r.getByte(), 0))
    } catch (Exception e) { return noEvents() }
}

// --- Job ---
translator(0xB0E1) { machine, packet ->
    try {
        def r = packet.streamReader
        if (r.getByte() != 1) return noEvents()
        return singleEvent(new JobJoinEvent(machine, r.getByte(), r.getByte(), Integer.toUnsignedLong(r.getInt())))
    } catch (Exception e) { return noEvents() }
}

translator(0xB0E2) { machine, packet ->
    try {
        if (packet.streamReader.getByte() != 1) return noEvents()
        return singleEvent(new JobLeaveEvent(machine))
    } catch (Exception e) { return noEvents() }
}

translator(0xB0E3) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = r.getByte() == 0x01
        String jobAlias = ""
        if (success) { r.getByte(); jobAlias = r.getString() }
        return singleEvent(new JobAliasUpdateEvent(machine, success, jobAlias))
    } catch (Exception e) { return noEvents() }
}

translator(0x30E6) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new JobExperienceUpdateEvent(machine, r.getByte(), r.getUnsignedByte(), r.getInt() & 0xFFFFFFFFL))
    } catch (Exception e) { return noEvents() }
}

// --- Alchemy ---
translator(0xB150) { machine, packet ->
    try {
        def r = packet.streamReader
        byte result = r.getByte()
        if (result == 2) return singleEvent(new AlchemyResultEvent(machine, false, (byte)1, (byte)0, false, false, null))
        byte action = r.getByte()
        if (action == 1) return singleEvent(new AlchemyResultEvent(machine, false, (byte)1, (byte)0, false, true, null))
        boolean isSuccess = r.getBoolean()
        byte slot = r.getByte()
        boolean destroyed = !isSuccess && r.getBoolean()
        return singleEvent(new AlchemyResultEvent(machine, isSuccess, (byte)1, slot, destroyed, false, null))
    } catch (Exception e) { return noEvents() }
}

translator(0xB151) { machine, packet ->
    try {
        def r = packet.streamReader
        byte result = r.getByte()
        if (result == 2) return singleEvent(new AlchemyResultEvent(machine, false, (byte)2, (byte)0, false, false, null))
        byte action = r.getByte()
        if (action == 1) return singleEvent(new AlchemyResultEvent(machine, false, (byte)2, (byte)0, false, true, null))
        boolean isSuccess = r.getBoolean()
        byte slot = r.getByte()
        boolean destroyed = !isSuccess && r.getBoolean()
        return singleEvent(new AlchemyResultEvent(machine, isSuccess, (byte)2, slot, destroyed, false, null))
    } catch (Exception e) { return noEvents() }
}

translator(0x34AA) { machine, packet ->
    try {
        def r = packet.streamReader
        if (r.getByte() == 2) return singleEvent(MagicOptionUpdateEvent.failure(machine, r.getShort() & 0xFFFF))
        byte counter = r.getByte()
        if (counter != 0) return singleEvent(MagicOptionUpdateEvent.success(machine, r.getByte()))
        return noEvents()
    } catch (Exception e) { return noEvents() }
}

translator(0xB157) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte result = r.getByte()
        if (result == 2) return singleEvent(AlchemyDismantleEvent.builder().fullName(machine).timestamp(ts).result(result).errorCode((int)r.getShort()).build())
        return singleEvent(AlchemyDismantleEvent.builder().fullName(machine).timestamp(ts).result(result).build())
    } catch (Exception e) { return noEvents() }
}

// --- Teleport ---
translator(0xB05A) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = r.getByte() == 0x01
        int destinationId = success ? r.getInt() : 0
        return singleEvent(new TeleportResponseEvent(machine, success, destinationId))
    } catch (Exception e) { return noEvents() }
}

// --- Academy ---
translator(0xB47D) { machine, packet ->
    try {
        def r = packet.streamReader
        if (r.getByte() != 1) return noEvents()
        byte pageCount = r.getByte()
        byte pageIndex = r.getByte()
        byte matchCount = r.getByte()
        def matches = (0..<matchCount).collect {
            def b = AcademyMatchingListEvent.AcademyMatch.builder()
            b.number(r.getInt())
            r.getInt()
            b.countryType(r.getByte())
            b.title(PacketReaderUtils.readString(r))
            r.getInt()
            r.getInt()
            b.displayLevel(r.getByte()).leaderLevel(r.getByte()).leaderId(r.getInt())
            b.leaderName(PacketReaderUtils.readString(r))
            b.graduatedCount(r.getInt()).honorRank(r.getByte())
            r.getLong()
            b.build()
        }
        return singleEvent(AcademyMatchingListEvent.builder().fullName(machine).pageCount(pageCount).pageIndex(pageIndex).matches(matches).build())
    } catch (Exception e) { return noEvents() }
}

translator(0x3C80) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte updateType = r.getByte()
        if (updateType == 5) {
            int charId = r.getInt()
            r.getByte()
            r.getByte()
            r.getByte()
            return singleEvent(AcademyUpdateEvent.builder().fullName(machine).timestamp(ts).updateType(updateType).charId(charId).newHonorBalance(r.getInt()).build())
        }
        return noEvents()
    } catch (Exception e) { return noEvents() }
}

// --- PK ---
translator(0x30CD) { machine, packet ->
    try {
        long ts = System.currentTimeMillis()
        return singleEvent(PKUpdateEvent.builder().fullName(machine).timestamp(ts).type(PKUpdateEvent.PKUpdateType.PENALTY).penaltyPoints(packet.streamReader.getInt()).build())
    } catch (Exception e) { return noEvents() }
}

translator(0x30CE) { machine, packet ->
    try {
        long ts = System.currentTimeMillis()
        return singleEvent(PKUpdateEvent.builder().fullName(machine).timestamp(ts).type(PKUpdateEvent.PKUpdateType.DAILY).dailyPk(packet.streamReader.getByte()).build())
    } catch (Exception e) { return noEvents() }
}

translator(0x30D3) { machine, packet ->
    try {
        long ts = System.currentTimeMillis()
        return singleEvent(PKUpdateEvent.builder().fullName(machine).timestamp(ts).type(PKUpdateEvent.PKUpdateType.LEVEL).totalPk((int)packet.streamReader.getShort()).build())
    } catch (Exception e) { return noEvents() }
}

// --- TAP ---
translator(0xB4DF) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte count = r.getByte()
        def entries = (0..<count).collect {
            TapInfoEvent.TapEntry.builder().entryType(r.getByte()).fromMonth(r.getByte()).fromDay(r.getByte()).fromHour(r.getByte()).fromMinute(r.getByte()).toMonth(r.getByte()).toDay(r.getByte()).toHour(r.getByte()).toMinute(r.getByte()).traderUnionPoints(r.getInt()).thiefUnionPoints(r.getInt()).build()
        }
        return singleEvent(TapInfoEvent.builder().fullName(machine).timestamp(ts).entries(entries).build())
    } catch (Exception e) { return noEvents() }
}

translator(0xB4E0) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        def entries = []
        try {
            while (true) {
                entries << TapUpdateEvent.TapEntryPoints.builder().traderUnionPoints(r.getInt()).thiefUnionPoints(r.getInt()).build()
            }
        } catch (Exception ignored) {}
        return singleEvent(TapUpdateEvent.builder().fullName(machine).timestamp(ts).entries(entries).build())
    } catch (Exception e) { return noEvents() }
}

// --- DamageEffect ---
translator(0x3058) { machine, packet ->
    try {
        def r = packet.streamReader
        int targetEntityId = r.getInt()
        byte damageType = r.getByte()
        int damageAmount = r.getInt()
        return singleEvent(new DamageEffectEvent(machine, targetEntityId, damageAmount, damageType))
    } catch (Exception e) { return noEvents() }
}

// --- BerserkConfirm ---
translator(0xB0A7) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = r.getBoolean()
        byte berserkLevel = success ? r.getByte() : 0
        return singleEvent(new BerserkConfirmEvent(machine, success, berserkLevel))
    } catch (Exception e) { return noEvents() }
}

// --- FRPVPUpdate ---
translator(0xB516) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte result = r.getByte()
        def builder = FRPVPUpdateEvent.builder().fullName(machine).timestamp(ts).result(result)
        if (result == 0x01) builder.uniqueId(r.getInt()).mode(r.getByte())
        else if (result == 0x02) builder.errorCode((int)r.getShort())
        return singleEvent(builder.build())
    } catch (Exception e) { return noEvents() }
}
