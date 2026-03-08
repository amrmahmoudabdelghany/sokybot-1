import org.sokybot.gameevents.events.inventory.*
import org.sokybot.gameevents.events.combat.PickupAnimationEvent

translator(0x3040) { machine, packet ->
    try {
        def r = packet.streamReader
        byte sourceSlot = r.getByte()
        byte updateFlags = r.getByte()
        Integer itemId = null
        Byte optLevel = null
        Integer quantity = null
        Integer durability = null
        if ((updateFlags & InventoryItemUpdateEvent.FLAG_REF_OBJ_ID) != 0) itemId = r.getInt()
        if ((updateFlags & InventoryItemUpdateEvent.FLAG_OPT_LEVEL) != 0) optLevel = r.getByte()
        if ((updateFlags & InventoryItemUpdateEvent.FLAG_VARIANCE) != 0) r.getLong()
        if ((updateFlags & InventoryItemUpdateEvent.FLAG_QUANTITY) != 0) quantity = r.getShort() & 0xFFFF
        if ((updateFlags & InventoryItemUpdateEvent.FLAG_DURABILITY) != 0) durability = r.getInt()
        if ((updateFlags & InventoryItemUpdateEvent.FLAG_STATE) != 0) r.getByte()
        if ((updateFlags & InventoryItemUpdateEvent.FLAG_MAG_PARAMS) != 0) {
            int n = r.getByte() & 0xFF
            n.times { r.getInt(); r.getInt() }
        }
        return singleEvent(new InventoryItemUpdateEvent(machine, sourceSlot, updateFlags, itemId, optLevel, quantity, durability))
    } catch (Exception e) { return noEvents() }
}

translator(0xB034) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = (r.getByte() == 0x01)
        if (!success) return singleEvent(new InventoryOperationEvent(machine, (byte)0, false, r.getByte(), null, null, null, null, null))
        return singleEvent(new InventoryOperationEvent(machine, r.getByte(), true, (byte)0, null, null, null, null, null))
    } catch (Exception e) { return noEvents() }
}

translator(0x3038) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new EquipItemVisualEvent(machine, r.getInt(), r.getByte(), r.getInt(), r.getByte()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3039) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new UnequipItemVisualEvent(machine, r.getInt(), r.getByte()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3036) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new PickupAnimationEvent(machine, r.getInt(), r.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0xB04C) { machine, packet ->
    try {
        def r = packet.streamReader
        if (r.getByte() != 1) return noEvents()
        return singleEvent(new ItemUseEvent(machine, r.getByte(), r.getShort() & 0xFFFF))
    } catch (Exception e) { return noEvents() }
}

translator(0x3052) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new ItemDurabilityUpdateEvent(machine, r.getByte(), r.getInt() & 0xFFFFFFFFL))
    } catch (Exception e) { return noEvents() }
}

translator(0x3092) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new InventorySizeUpdateEvent(machine, r.getByte(), r.getByte() & 0xFF))
    } catch (Exception e) { return noEvents() }
}

translator(0xB03E) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = (r.getByte() == 0x01)
        byte slot = success ? r.getByte() : 0
        long cost = success ? r.getLong() : 0
        return singleEvent(new ItemRepairEvent(machine, success, slot, cost))
    } catch (Exception e) { return noEvents() }
}

translator(0x325F) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new ItemPerkAddEvent(machine, r.getInt(), r.getInt(), r.getInt(), r.getInt(), r.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3261) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new ItemPerkRemoveEvent(machine, r.getInt(), r.getInt(), r.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0x304D) { machine, packet ->
    try {
        return singleEvent(new ItemOwnershipRemovedEvent(machine, packet.streamReader.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3201) { machine, packet ->
    try {
        return singleEvent(new AmmoUpdateEvent(machine, packet.streamReader.getShort() & 0xFFFF))
    } catch (Exception e) { return noEvents() }
}
