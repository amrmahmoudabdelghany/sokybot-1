import org.sokybot.gameevents.script.CharacterDataParser
import org.sokybot.gameevents.events.character.*

// Character data begin (opcode 0x34A5) - start chunked transaction
translator(0x34A5) { machine, packet ->
    try {
        def chunkManager = getChunkManager(machine)
        if (chunkManager != null) chunkManager.begin(0x34A5)
        return singleEvent(new CharacterDataBeginEvent(machine))
    } catch (Exception e) {
        return noEvents()
    }
}

// Character data end (opcode 0x34A6) - full parse with events
translator(0x34A6) { machine, packet ->
    try {
        def reader = packet.streamReader
        def events = []
        def handlers = [
            onInventoryItem: { byte slot, int itemId ->
                def itemName = lookup?.findItem(itemId)?.map({ it.name }).orElse(null)
                events << new CharacterItemLoadedEvent(machine, slot, itemId, itemName, 1, 0, false)
            },
            onAvatarItem: { byte slot, int itemId ->
                def itemName = lookup?.findItem(itemId)?.map({ it.name }).orElse(null)
                events << new CharacterItemLoadedEvent(machine, slot, itemId, itemName, 1, 0, true)
            },
            onMastery: { int masteryId, int masteryLevel ->
                def masteryName = lookup?.findMasteryName(masteryId).orElse(null)
                events << new CharacterMasteryLoadedEvent(machine, masteryId, masteryName, masteryLevel)
            },
            onSkill: { int skillRefId, boolean isEnabled ->
                def skillName = lookup?.findSkill(skillRefId)?.map({ it.name }).orElse(null)
                events << new CharacterSkillLoadedEvent(machine, skillRefId, skillName, 0, isEnabled)
            },
            onBuff: { int buffRefId, int duration ->
                def buffName = lookup?.findSkill(buffRefId)?.map({ it.name }).orElse(null)
                events << new CharacterBuffLoadedEvent(machine, buffRefId, buffName, duration, false)
            }
        ] as CharacterDataParser.Handlers
        def data = CharacterDataParser.parse(reader, lookup, handlers)
        events << new CharacterLoadedEvent(machine, data.uniqueId(), data.refId(), data.characterName(),
            data.level(), data.maxLevel(), data.experience(), data.gold(), data.skillPoints(), data.statPoints(),
            data.currentHP(), data.currentMP(), data.lifeState(), data.debuffStatus(), data.motionState(), data.characterStatus(),
            data.walkSpeed(), data.runSpeed(), data.xSector(), data.ySector(), data.xOffset(), data.yOffset(), data.zOffset(), data.angle(),
            data.inventoryItemCount(), data.avatarItemCount(), data.masteryCount(), data.skillCount(),
            data.activeBuffCount(), data.hotKeyCount(), data.jobName(), data.jobType(), data.jobLevel(), data.jobExp(),
            data.pvpState(), data.hasTransport(), data.inCombat())
        return events
    } catch (Exception e) {
        return noEvents()
    }
}

// Character loaded (opcode 0x3013) - single packet, skip handlers
translator(0x3013) { machine, packet ->
    try {
        def data = CharacterDataParser.parse(packet.streamReader, lookup, null)
        return singleEvent(new CharacterLoadedEvent(machine, data.uniqueId(), data.refId(), data.characterName(),
            data.level(), data.maxLevel(), data.experience(), data.gold(), data.skillPoints(), data.statPoints(),
            data.currentHP(), data.currentMP(), data.lifeState(), data.debuffStatus(), data.motionState(), data.characterStatus(),
            data.walkSpeed(), data.runSpeed(), data.xSector(), data.ySector(), data.xOffset(), data.yOffset(), data.zOffset(), data.angle(),
            data.inventoryItemCount(), data.avatarItemCount(), data.masteryCount(), data.skillCount(),
            data.activeBuffCount(), data.hotKeyCount(), data.jobName(), data.jobType(), data.jobLevel(), data.jobExp(),
            data.pvpState(), data.hasTransport(), data.inCombat()))
    } catch (Exception e) {
        return noEvents()
    }
}

// Character info (opcode 0x303D)
translator(0x303D) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new CharacterInfoEvent(machine,
            r.getInt(), r.getInt(), r.getInt(), r.getInt(),
            r.getShort() & 0xFFFF, r.getShort() & 0xFFFF, r.getShort() & 0xFFFF, r.getShort() & 0xFFFF,
            r.getInt(), r.getInt(), r.getShort(), r.getShort()))
    } catch (Exception e) {
        return noEvents()
    }
}

// Character join (opcode 0xB001)
translator(0xB001) { machine, packet ->
    try {
        def r = packet.streamReader
        byte result = r.getByte()
        boolean success = (result == 0x01)
        Integer errorCode = success ? null : (r.getShort() & 0xFFFF)
        return singleEvent(CharacterJoinEvent.builder()
            .fullName(machine).timestamp(System.currentTimeMillis())
            .success(success).errorCode(errorCode).build())
    } catch (Exception e) {
        return noEvents()
    }
}

// Character selection action (opcode 0xB007)
translator(0xB007) { machine, packet ->
    try {
        def r = packet.streamReader
        byte action = r.getByte()
        byte result = r.getByte()
        def characters = []
        Integer errorCode = null
        if (result == 1) {
            try {
                byte characterCount = r.getByte()
                for (int i = 0; i < characterCount; i++) {
                    int refObjId = r.getInt()
                    String name = r.getString()
                    byte scale = r.getByte()
                    byte level = r.getByte()
                    long expOffset = r.getLong()
                    int str = r.getShort() & 0xFFFF
                    int intell = r.getShort() & 0xFFFF
                    int statPoint = r.getShort() & 0xFFFF
                    int curHP = r.getInt()
                    int curMP = r.getInt()
                    boolean deleting = r.getByte() != 0
                    int deleteTime = deleting ? r.getInt() : 0
                    byte guildMemberClass = r.getByte()
                    boolean guildRenameRequired = r.getByte() != 0
                    String curGuildName = guildRenameRequired ? r.getString() : null
                    byte academyMemberClass = r.getByte()
                    byte itemCount = r.getByte()
                    def items = (0..<itemCount).collect { CharacterSelectionActionEvent.SelectedCharacterItem.builder().refItemId(r.getInt()).plus(r.getByte()).build() }
                    byte avatarItemCount = r.getByte()
                    def avatarItems = (0..<avatarItemCount).collect { CharacterSelectionActionEvent.SelectedCharacterItem.builder().refItemId(r.getInt()).plus(r.getByte()).build() }
                    characters << CharacterSelectionActionEvent.CharSelectionEntry.builder()
                        .refObjId(refObjId).name(name).scale(scale).level(level).expOffset(expOffset)
                        .strength(str).intelligence(intell).statPoint(statPoint).curHP(curHP).curMP(curMP)
                        .deleting(deleting).deleteTime(deleteTime).guildMemberClass(guildMemberClass)
                        .guildRenameRequired(guildRenameRequired).curGuildName(curGuildName).academyMemberClass(academyMemberClass)
                        .items(items).avatarItems(avatarItems).build()
                }
            } catch (Exception ignored) {}
        } else if (result == 2) {
            errorCode = r.getShort() & 0xFFFF
        }
        return singleEvent(CharacterSelectionActionEvent.builder()
            .fullName(machine).timestamp(System.currentTimeMillis())
            .action(action).result(result).characters(characters).errorCode(errorCode).build())
    } catch (Exception e) {
        return noEvents()
    }
}

// Character rename ack (opcode 0xB450)
translator(0xB450) { machine, packet ->
    try {
        def r = packet.streamReader
        byte renameAction = r.getByte()
        byte result = r.getByte()
        int errorCode = (result == 2) ? (r.getShort() & 0xFFFF) : 0
        return singleEvent(new CharacterRenameAckEvent(machine, renameAction, result, errorCode))
    } catch (Exception e) {
        return noEvents()
    }
}
