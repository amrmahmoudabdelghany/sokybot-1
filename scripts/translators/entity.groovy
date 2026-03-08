import org.sokybot.gameevents.script.SpawnDataReader
import org.sokybot.gameevents.dto.GamePosition
import org.sokybot.gameevents.events.entity.*
import org.sokybot.gameevents.events.spawn.*

// Entity spawn (opcode 0x3015) - items and monsters via SpawnDataReader + lookup
translator(0x3015) { machine, packet ->
    try {
        def reader = packet.streamReader
        int refId = reader.getInt()
        def dataReader = new SpawnDataReader(reader, lookup)
        def itemOpt = lookup.findItem(refId)
        if (itemOpt.isPresent()) {
            def itemData = dataReader.readDropItem(itemOpt.get())
            return singleEvent(new ItemSpawnEvent(machine, itemData))
        }
        def npcOpt = lookup.findNPC(refId)
        if (npcOpt.isPresent()) {
            def npc = npcOpt.get()
            if (npc.getType().name().startsWith("Player")) return noEvents()
            if (npc.getLongId().contains("MOB_")) {
                def monsterData = dataReader.readMonster(npc)
                return singleEvent(new MonsterSpawnEvent(machine, monsterData))
            }
        }
        return noEvents()
    } catch (Exception e) {
        return noEvents()
    }
}

// Entity despawn (opcode 0x3016)
translator(0x3016) { machine, packet ->
    try {
        int entityId = packet.streamReader.getInt()
        return singleEvent(new EntityDespawnEvent(machine, entityId))
    } catch (Exception e) {
        return noEvents()
    }
}

// Group spawn begin (opcode 0x3017)
translator(0x3017) { machine, packet ->
    try {
        def reader = packet.streamReader
        byte spawnType = reader.getByte()
        short count = reader.getShort()
        return singleEvent(new GroupSpawnBeginEvent(machine, spawnType, count))
    } catch (Exception e) {
        return noEvents()
    }
}

// Group spawn end (opcode 0x3018)
translator(0x3018) { machine, packet ->
    return singleEvent(new GroupSpawnEndEvent(machine))
}

// Group spawn data (opcode 0x3019) - multiple monsters
translator(0x3019) { machine, packet ->
    try {
        def reader = packet.streamReader
        int count = reader.getShort() & 0xFFFF
        def events = []
        def dataReader = new SpawnDataReader(reader, lookup)
        for (int i = 0; i < count; i++) {
            int refId = reader.getInt()
            lookup.findNPC(refId).ifPresent { entity ->
                def data = dataReader.readMonster(entity)
                events << new EntitySpawnEvent(machine, data.uniqueId, data.refId, data.name,
                    data.xSector, data.ySector, data.xOffset, data.yOffset, data.zOffset,
                    data.angle, data.position)
            }
        }
        return events
    } catch (Exception e) {
        return noEvents()
    }
}

// Entity movement (opcode 0xB021)
translator(0xB021) { machine, packet ->
    try {
        def reader = packet.streamReader
        int entityId = reader.getInt()
        boolean hasDestination = reader.getBoolean()
        Byte movementType = null
        GamePosition destination = null
        Integer destXSector = null
        Integer destYSector = null
        Byte skyClickFlag = null
        Byte angleAction = null
        if (hasDestination) {
            int destXSectorVal = reader.getUnsignedByte()
            int destYSectorVal = reader.getUnsignedByte()
            boolean inCave = (destYSectorVal == 0x80)
            float destXOffset, destZOffset, destYOffset
            if (inCave) {
                destXOffset = reader.getInt()
                destZOffset = reader.getInt()
                destYOffset = reader.getInt()
            } else {
                destXOffset = reader.getShort()
                destZOffset = reader.getShort()
                destYOffset = reader.getShort()
            }
            destination = new GamePosition(destXOffset, destZOffset, destYOffset, (byte) 0)
            destXSector = destXSectorVal
            destYSector = destYSectorVal
        } else {
            skyClickFlag = reader.getByte()
            angleAction = reader.getByte()
        }
        GamePosition currentPosition = null
        Integer currentXSector = null
        Integer currentYSector = null
        Short currentAngle = null
        boolean hasOrigin = reader.getBoolean()
        if (hasOrigin) {
            int xSector = reader.getUnsignedByte()
            int ySector = reader.getUnsignedByte()
            float xOffset = reader.getShort()
            float zOffset = reader.getShort()
            short angle = reader.getShort()
            float yOffset = reader.getShort()
            currentPosition = new GamePosition(xOffset, zOffset, yOffset, (byte) 0)
            currentXSector = xSector
            currentYSector = ySector
            currentAngle = angle
        }
        return singleEvent(new EntityMovementEvent(machine, entityId, hasDestination,
            destination, destXSector, destYSector, currentPosition, currentXSector, currentYSector,
            currentAngle, movementType, skyClickFlag, angleAction))
    } catch (Exception e) {
        return noEvents()
    }
}

// Entity stopped (opcode 0xB023)
translator(0xB023) { machine, packet ->
    try {
        int entityId = packet.streamReader.getInt()
        return singleEvent(new EntityStoppedEvent(machine, entityId))
    } catch (Exception e) {
        return noEvents()
    }
}

// Entity angle update (opcode 0xB024)
translator(0xB024) { machine, packet ->
    try {
        def reader = packet.streamReader
        int entityId = reader.getInt()
        short newAngle = reader.getShort()
        return singleEvent(new EntityAngleUpdateEvent(machine, entityId, newAngle))
    } catch (Exception e) {
        return noEvents()
    }
}

// Speed update (opcode 0x30D0)
translator(0x30D0) { machine, packet ->
    try {
        def reader = packet.streamReader
        int entityId = reader.getInt()
        float walkSpeed = reader.getFloat()
        float runSpeed = reader.getFloat()
        return singleEvent(new EntitySpeedUpdateEvent(machine, entityId, walkSpeed, runSpeed))
    } catch (Exception e) {
        return noEvents()
    }
}

// Entity selected (opcode 0xB045)
translator(0xB045) { machine, packet ->
    try {
        def reader = packet.streamReader
        boolean hasSelection = reader.getBoolean()
        if (hasSelection) {
            int selectedEntityId = reader.getInt()
            Integer currentHP = null
            if (reader.getBoolean()) currentHP = reader.getInt()
            return singleEvent(new EntitySelectedEvent(machine, selectedEntityId, currentHP))
        }
        return noEvents()
    } catch (Exception e) {
        return noEvents()
    }
}

// Entity deselected (opcode 0xB04B)
translator(0xB04B) { machine, packet ->
    try {
        if (packet.streamReader.getByte() != 0x01) return noEvents()
        return singleEvent(new EntityDeselectedEvent(machine))
    } catch (Exception e) {
        return noEvents()
    }
}
