import org.sokybot.gameevents.events.character.*
import org.sokybot.gameevents.events.entity.*
import org.sokybot.gameevents.events.buff.*
import org.sokybot.gameevents.events.skill.*

// Character death (opcode 0x3053/0x3011)
translator(0x3011) { machine, packet ->
    def reader = packet.streamReader
    byte flag = reader.getByte()
    flag == (byte) 0x04 ? singleEvent(new CharacterDeathEvent(machine, null)) : noEvents()
}

// Buff applied (opcode 0x30BD)
translator(0x30BD) { machine, packet ->
    def reader = packet.streamReader
    int targetId = reader.getInt()
    int buffId = reader.getInt()
    int duration = reader.getInt()
    singleEvent(new BuffAppliedEvent(machine, buffId, targetId, duration))
}

// Buff removed (opcode 0x30BE)
translator(0x30BE) { machine, packet ->
    def reader = packet.streamReader
    int buffId = reader.getInt()
    singleEvent(new BuffRemovedEvent(machine, buffId))
}

// Mastery level up (0xB0A2) and Skill level up (0xB0A1) in skill.groovy
