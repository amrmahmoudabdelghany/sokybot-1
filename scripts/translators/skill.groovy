import org.sokybot.gameevents.events.skill.*

// Skill cast started (opcode 0xB070)
translator(0xB070) { machine, packet ->
    try {
        def r = packet.streamReader
        byte result = r.getByte()
        if (result == 1) {
            r.getByte()
            r.getByte()
            int skillId = r.getInt()
            int casterId = r.getInt()
            r.getInt()
            int targetId = r.getInt()
            def skillName = lookup?.findSkill(skillId)?.map({ it.name }).orElse(null)
            return singleEvent(new SkillCastEvent(machine, true, skillId, skillName, casterId, targetId))
        }
        return singleEvent(new SkillCastEvent(machine, false, null, null, null, null))
    } catch (Exception e) {
        return noEvents()
    }
}

// Skill cast ended (opcode 0xB071)
translator(0xB071) { machine, packet ->
    try {
        def r = packet.streamReader
        int casterId = r.getInt()
        int skillId = r.getInt()
        def skillName = lookup?.findSkill(skillId)?.map({ it.name }).orElse(null)
        return singleEvent(new SkillCastEndEvent(machine, casterId, skillId, skillName))
    } catch (Exception e) {
        return noEvents()
    }
}

// Skill cast confirm (opcode 0xB074)
translator(0xB074) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = r.getBoolean()
        byte queuePosition = r.getByte()
        return singleEvent(new SkillCastConfirmEvent(machine, success, queuePosition))
    } catch (Exception e) {
        return noEvents()
    }
}

// Skill level up (opcode 0xB0A1) - also in combat.groovy, keep one place
translator(0xB0A1) { machine, packet ->
    try {
        int skillId = packet.streamReader.getInt()
        return singleEvent(new SkillLevelUpEvent(machine, true, skillId))
    } catch (Exception e) {
        return noEvents()
    }
}

// Mastery level up (opcode 0xB0A2)
translator(0xB0A2) { machine, packet ->
    try {
        def r = packet.streamReader
        int masteryId = r.getInt()
        int newLevel = r.getByte() & 0xFF
        return singleEvent(new MasteryLevelUpEvent(machine, true, masteryId, newLevel))
    } catch (Exception e) {
        return noEvents()
    }
}

// Skill withdraw (opcode 0xB202)
translator(0xB202) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = (r.getByte() == 0x01)
        int newSkillId = success ? r.getInt() : 0
        return singleEvent(new SkillWithdrawEvent(machine, success, 0, newSkillId))
    } catch (Exception e) {
        return noEvents()
    }
}

// Mastery level down (opcode 0xB203)
translator(0xB203) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = (r.getByte() == 1)
        int errorCode = success ? 0 : (r.getShort() & 0xFFFF)
        return singleEvent(new MasteryLevelDownEvent(machine, success, errorCode))
    } catch (Exception e) {
        return noEvents()
    }
}

// Skill points update (opcode 0x3843) - also in progression.groovy
translator(0x3843) { machine, packet ->
    try {
        int newSkillPoints = packet.streamReader.getInt()
        return singleEvent(new SkillPointsUpdateEvent(machine, newSkillPoints))
    } catch (Exception e) {
        return noEvents()
    }
}
