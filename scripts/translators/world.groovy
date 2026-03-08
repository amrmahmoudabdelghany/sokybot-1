import org.sokybot.gameevents.events.world.*
import org.sokybot.gameevents.events.arena.BattleArenaOperationEvent

translator(0x3020) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new CelestialPositionEvent(machine, r.getInt(), r.getShort()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3809) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new WeatherUpdateEvent(machine, r.getByte(), r.getUnsignedByte()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3027) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new CelestialUpdateEvent(machine, r.getShort() & 0xFFFF, r.getByte() & 0xFF, r.getByte() & 0xFF))
    } catch (Exception e) { return noEvents() }
}

translator(0x300C) { machine, packet ->
    try {
        def r = packet.streamReader
        byte result = r.getByte()
        if (result == 0) return noEvents()
        long ts = System.currentTimeMillis()
        if (result == 0x05) {
            r.getByte()
            return singleEvent(GameNotifyEvent.builder().fullName(machine).timestamp(ts).type(GameNotifyEvent.NotifyType.UNIQUE_SPAWNED).modelId(r.getInt()).build())
        }
        if (result == 0x06) {
            return singleEvent(GameNotifyEvent.builder().fullName(machine).timestamp(ts).type(GameNotifyEvent.NotifyType.UNIQUE_KILLED).modelId(r.getInt()).build())
        }
        return noEvents()
    } catch (Exception e) { return noEvents() }
}

translator(0x34D2) { machine, packet ->
    try {
        def r = packet.streamReader
        byte op = r.getByte()
        def data = [operation: op]
        switch (op) {
            case 0: case 1: case 2: case 3: case 0xD: case 0xE: case 0xF:
                data.matchType = r.getByte()
                data.gameTypeMask = r.getShort()
                break
            case 5: case 9: case 0xB: case 0xC:
                data.matchType = r.getByte()
                if (op == 9) {
                    data.gameResult = r.getByte()
                    data.coinCount = r.getByte()
                    data.skillExp = r.getInt()
                }
                break
            case 8:
                data.maxTime = r.getInt()
                break
            case (byte)0xFF:
                byte update = r.getByte()
                data.updateType = update
                if (update == 0) {
                    data.requestType = r.getByte()
                    data.gameType = r.getByte()
                } else if (update == 0x40) {
                    data.gainedPoints = r.getInt()
                } else if (update == 0x41) {
                    data.totalRed = r.getInt()
                    data.totalBlue = r.getInt()
                    data.rankCount = r.getByte() & 0xFF
                } else if (update == (byte)0xF0) {
                    data.maxTime = r.getInt()
                    data.elapsedTime = r.getInt()
                }
                break
        }
        return singleEvent(BattleArenaOperationEvent.builder().fullName(machine).operation(op).data(data).build())
    } catch (Exception e) { return noEvents() }
}

translator(0x385F) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte updateTypeByte = r.getByte()
        def type = SiegeUpdateEvent.SiegeUpdateType.fromValue(updateTypeByte)
        def builder = SiegeUpdateEvent.builder().fullName(machine).timestamp(ts).type(type)
        switch (type) {
            case SiegeUpdateEvent.SiegeUpdateType.INFO:
                byte fortressCount = r.getByte()
                def fortresses = (0..<fortressCount).collect {
                    int id = r.getInt()
                    String guildName = r.getString()
                    int guildId = r.getInt()
                    String leaderName = r.getString()
                    String instruction = r.getString()
                    int guildCrestRev = r.getInt()
                    int unionId = r.getInt()
                    int unionCrestRev = r.getInt()
                    boolean enterEnabled = r.getByte() != 0
                    int enterCD = enterEnabled ? r.getInt() : 0
                    boolean stoneEnabled = r.getByte() != 0
                    int stoneCD = stoneEnabled ? r.getInt() : 0
                    SiegeUpdateEvent.SiegeFortressInfo.builder().id(id).guildName(guildName).guildId(guildId).leaderName(leaderName).instruction(instruction).guildCrestRev(guildCrestRev).unionId(unionId).unionCrestRev(unionCrestRev).enterCountdownEnabled(enterEnabled).enterCountdown(enterCD).stoneCooldownEnabled(stoneEnabled).stoneCooldown(stoneCD).build()
                }
                builder.fortresses(fortresses).siegePeriod(r.getByte()).owningFortressID(r.getInt())
                break
            case SiegeUpdateEvent.SiegeUpdateType.TAX_RATE:
                builder.siegeId(r.getInt()).taxRate(r.getShort())
                break
            case SiegeUpdateEvent.SiegeUpdateType.OCCUPIED:
                builder.siegeId(r.getInt()).guildName(r.getString()).leaderName(r.getString()).instruction(r.getString()).guildId(r.getInt()).guildCrestRev(r.getInt()).unionId(r.getInt()).unionCrestRev(r.getInt())
                break
            case SiegeUpdateEvent.SiegeUpdateType.SEAL_DESTROYED:
                builder.siegeId(r.getInt())
                break
            case SiegeUpdateEvent.SiegeUpdateType.STRUCTURE_STATE:
                builder.siegeId(r.getInt()).structureUniqueID(r.getInt()).refEventStructID(r.getInt()).structureState(r.getShort())
                try { builder.guildName(r.getString()) } catch (Exception ignored) {}
                break
            case SiegeUpdateEvent.SiegeUpdateType.BATTLE_RANK:
                boolean self = r.getByte() != 0
                builder.self(self)
                if (!self) builder.playerName(r.getString())
                builder.rankLevel(r.getByte())
                break
            case SiegeUpdateEvent.SiegeUpdateType.STRUCTURE_INFO:
                builder.siegeId(r.getInt())
                byte strucCount = r.getByte()
                def structureInfos = (0..<strucCount).collect {
                    boolean occupied = r.getByte() != 0
                    String occGuild = occupied ? r.getString() : null
                    SiegeUpdateEvent.SiegeStructureInfo.builder().refEventStructID(r.getInt()).refObjID(r.getInt()).curHP(r.getInt()).state(r.getShort()).occupied(occupied).occupyingGuildName(occGuild).build()
                }
                builder.structures(structureInfos)
                break
            case SiegeUpdateEvent.SiegeUpdateType.BATTLE_RECORD:
                builder.siegeId(r.getInt()).killCount(r.getInt()).killedCount(r.getInt())
                break
        }
        return singleEvent(builder.build())
    } catch (Exception e) { return noEvents() }
}
