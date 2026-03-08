import org.sokybot.gameevents.events.stat.*

// Level up / promotion animation (opcode 0x3054)
translator(0x3054) { machine, packet ->
    def reader = packet.streamReader
    singleEvent(new LevelUpEvent(machine, reader.getInt()))
}

// Experience / SP update (opcode 0x3056)
translator(0x3056) { machine, packet ->
    def reader = packet.streamReader
    reader.getInt() // monsterId (unused)
    int currentExp = reader.getInt()
    int previousExp = reader.getInt()
    long expGained = currentExp - previousExp
    singleEvent(new ExpUpdateEvent(machine, expGained, currentExp, false))
}

// Skill points update (0x3843) in skill.groovy

// Gold update (opcode 0x304E, type GOLD=1)
translator(0x304E) { machine, packet ->
    def reader = packet.streamReader
    byte gainType = reader.getByte()
    if (gainType == (byte) 1) {
        long newGold = reader.getLong()
        return singleEvent(new GoldUpdateEvent(machine, newGold))
    }
    return noEvents()
}
