import org.sokybot.gameevents.events.session.*

// Logout success (opcode 0x300A)
translator(0x300A) { machine, packet ->
    singleEvent(new LogoutEvent(machine))
}

// Game ready (opcode 0x3077) - item/skill cooldowns
translator(0x3077) { machine, packet ->
    def reader = packet.streamReader
    def itemCooldowns = []
    int itemCount = reader.getUnsignedByte()
    for (int i = 0; i < itemCount; i++) {
        itemCooldowns << new GameReadyEvent.CooldownInfo(reader.getInt(), reader.getInt())
    }
    def skillCooldowns = []
    int skillCount = reader.getUnsignedByte()
    for (int i = 0; i < skillCount; i++) {
        skillCooldowns << new GameReadyEvent.CooldownInfo(reader.getInt(), reader.getInt())
    }
    singleEvent(new GameReadyEvent(machine, itemCooldowns, skillCooldowns))
}

// Teleport complete (opcode 0x34B5)
translator(0x34B5) { machine, packet ->
    singleEvent(new TeleportCompleteEvent(machine))
}
