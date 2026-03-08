import org.sokybot.gameevents.events.combat.MountStateUpdateEvent
import org.sokybot.gameevents.events.cos.*

translator(0xB0CB) { machine, packet ->
    try {
        def r = packet.streamReader
        if (r.getByte() != 0x01) return noEvents()
        return singleEvent(new MountStateUpdateEvent(machine, r.getInt(), r.getByte() != 0, r.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0x30C8) { machine, packet ->
    try {
        def r = packet.streamReader
        int uniqueId = r.getInt()
        int objectId = r.getInt()
        int hp = r.getInt()
        int maxHp = r.getInt()
        return singleEvent(new CosDataEvent(machine, uniqueId, objectId, null, hp, maxHp, 0))
    } catch (Exception e) { return noEvents() }
}

translator(0x30C9) { machine, packet ->
    try {
        def r = packet.streamReader
        int uniqueId = r.getInt()
        byte typeId = r.getByte()
        def updateType = CosUpdateType.fromTypeId(typeId & 0xFF)
        if (updateType == null) return singleEvent(new CosUpdateEvent(machine, uniqueId, null))
        long experience = 0
        int sourceUniqueId = 0
        int hungerPoints = 0
        String newName = null
        int newObjectId = 0
        switch (updateType) {
            case CosUpdateType.TERMINATE:
                break
            case CosUpdateType.INVENTORY:
                break
            case CosUpdateType.EXPERIENCE:
                experience = r.getLong()
                sourceUniqueId = r.getInt()
                break
            case CosUpdateType.HUNGER:
                hungerPoints = r.getShort() & 0xFFFF
                break
            case CosUpdateType.NAME_CHANGE:
                newName = r.getString()
                break
            case CosUpdateType.MODEL_CHANGE:
                newObjectId = r.getInt()
                break
            case CosUpdateType.FELLOW_KILL_EXP:
                experience = r.getLong()
                r.getLong()
                r.getInt()
                sourceUniqueId = r.getInt()
                break
        }
        return singleEvent(new CosUpdateEvent(machine, uniqueId, updateType, experience, sourceUniqueId, hungerPoints, newName, newObjectId))
    } catch (Exception e) { return noEvents() }
}
