import org.sokybot.gameevents.events.stat.*
import org.sokybot.gameevents.events.entity.EntityHPMPUpdateEvent

// Stat points update STR (0xB050)
translator(0xB050) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = (r.getByte() == 0x01)
        int remainingPoints = success ? (r.getShort() & 0xFFFF) : 0
        return singleEvent(new StatPointsUpdateEvent(machine, StatPointsUpdateEvent.StatType.STRENGTH, success, remainingPoints))
    } catch (Exception e) { return noEvents() }
}

// Stat points update INT (0xB051)
translator(0xB051) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = (r.getByte() == 0x01)
        int remainingPoints = success ? (r.getShort() & 0xFFFF) : 0
        return singleEvent(new StatPointsUpdateEvent(machine, StatPointsUpdateEvent.StatType.INTELLIGENCE, success, remainingPoints))
    } catch (Exception e) { return noEvents() }
}

// Hwan level update (0x30DF)
translator(0x30DF) { machine, packet ->
    try {
        def r = packet.streamReader
        int entityId = r.getInt()
        byte hwanLevel = r.getByte()
        int hwanProgress = r.getInt()
        return singleEvent(new HwanLevelUpdateEvent(machine, entityId, hwanLevel, hwanProgress))
    } catch (Exception e) { return noEvents() }
}

// Attack speed update (0x3200)
translator(0x3200) { machine, packet ->
    try {
        def r = packet.streamReader
        int entityId = r.getInt()
        int attackSpeed = r.getInt()
        return singleEvent(new AttackSpeedUpdateEvent(machine, entityId, attackSpeed))
    } catch (Exception e) { return noEvents() }
}

// HP/MP update (0x3057)
translator(0x3057) { machine, packet ->
    try {
        def r = packet.streamReader
        int currentHP = r.getInt()
        int currentMP = r.getInt()
        return singleEvent(new HPMPUpdateEvent(machine, currentHP, currentMP))
    } catch (Exception e) { return noEvents() }
}

// Life state update (0x30BF)
translator(0x30BF) { machine, packet ->
    try {
        def r = packet.streamReader
        byte lifeState = r.getByte()
        return singleEvent(new LifeStateUpdateEvent(machine, lifeState))
    } catch (Exception e) { return noEvents() }
}

// Fellow stat update (0x3422)
translator(0x3422) { machine, packet ->
    try {
        def r = packet.streamReader
        int uniqueId = r.getInt()
        int maxHP = r.getInt()
        int curHP = r.getInt()
        int maxMP = r.getInt()
        int curMP = r.getInt()
        return singleEvent(new FellowStatUpdateEvent(machine, uniqueId, maxHP, curHP, maxMP, curMP))
    } catch (Exception e) { return noEvents() }
}
