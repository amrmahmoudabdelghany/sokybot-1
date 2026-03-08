import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.events.exchange.*
import org.sokybot.gameevents.events.stall.*
import org.sokybot.gameevents.events.consignment.*

// --- Exchange (0x3085, 0x3086, 0x3087, 0x3088, 0x3089) ---
translator(0x3085) { machine, packet ->
    try {
        return singleEvent(new ExchangeStartedEvent(machine, packet.streamReader.getInt()))
    } catch (Exception e) { return noEvents() }
}

translator(0x3088) { machine, packet ->
    return singleEvent(new ExchangeCancelledEvent(machine))
}

translator(0x3086) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new ExchangeConfirmedEvent(machine, r.getInt(), r.getByte() == 0))
    } catch (Exception e) { return noEvents() }
}

translator(0x3087) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new ExchangeApprovedEvent(machine, r.getInt(), r.getByte() == 0x01))
    } catch (Exception e) { return noEvents() }
}

translator(0x3089) { machine, packet ->
    try {
        def r = packet.streamReader
        return singleEvent(new ExchangeUpdateEvent(machine, r.getInt(), r.getByte() == 0, r.getByte(), r.getInt(), r.getShort() & 0xFFFF, r.getLong()))
    } catch (Exception e) { return noEvents() }
}

// --- Stall (0x30B7, 0x30B8, 0x30B9, 0x30BB, 0xB0BA) ---
translator(0x30B7) { machine, packet ->
    try {
        return singleEvent(new StallEvent(machine, packet.streamReader.getInt(), StallEvent.StallEventType.ACTION, null))
    } catch (Exception e) { return noEvents() }
}

translator(0x30B8) { machine, packet ->
    try {
        return singleEvent(new StallEvent(machine, packet.streamReader.getInt(), StallEvent.StallEventType.CREATED, null))
    } catch (Exception e) { return noEvents() }
}

translator(0x30B9) { machine, packet ->
    try {
        return singleEvent(new StallEvent(machine, packet.streamReader.getInt(), StallEvent.StallEventType.DESTROYED, null))
    } catch (Exception e) { return noEvents() }
}

translator(0x30BB) { machine, packet ->
    try {
        def r = packet.streamReader
        int entityId = r.getInt()
        int nameLen = r.getShort() & 0xFFFF
        String stallName = r.getUnicodeString(nameLen)
        return singleEvent(new StallEvent(machine, entityId, StallEvent.StallEventType.NAME_CHANGED, stallName))
    } catch (Exception e) { return noEvents() }
}

translator(0xB0BA) { machine, packet ->
    try {
        def r = packet.streamReader
        byte result = r.getByte()
        byte typeRaw = r.getByte()
        def type = (typeRaw == 1) ? StallUpdateEvent.StallUpdateType.UPDATE_ITEM : (typeRaw == 2) ? StallUpdateEvent.StallUpdateType.ADD_ITEM : (typeRaw == 3) ? StallUpdateEvent.StallUpdateType.REMOVE_ITEM : (typeRaw == 5) ? StallUpdateEvent.StallUpdateType.STATE : (typeRaw == 6) ? StallUpdateEvent.StallUpdateType.MESSAGE : (typeRaw == 7) ? StallUpdateEvent.StallUpdateType.NAME : StallUpdateEvent.StallUpdateType.UNKNOWN
        def builder = StallUpdateEvent.builder().fullName(machine).timestamp(System.currentTimeMillis()).result(result).type(type)
        switch (type) {
            case StallUpdateEvent.StallUpdateType.UPDATE_ITEM:
                builder.slot(r.getByte()).stackCount((int) r.getShort()).price(r.getLong()).errorCode((int) r.getShort())
                break
            case StallUpdateEvent.StallUpdateType.ADD_ITEM:
            case StallUpdateEvent.StallUpdateType.REMOVE_ITEM:
                builder.errorCode((int) r.getShort())
                break
            case StallUpdateEvent.StallUpdateType.STATE:
                builder.isOpen(r.getBoolean()).errorCode((int) r.getShort())
                break
            case StallUpdateEvent.StallUpdateType.MESSAGE:
                builder.message(r.getString())
                break
            case StallUpdateEvent.StallUpdateType.NAME:
                builder.name(r.getString())
                break
        }
        return singleEvent(builder.build())
    } catch (Exception e) { return noEvents() }
}

// --- Consignment: list (0xB50E), register (0xB508), buy (0xB50A), update (0x350D), search (0xB50C), detail (0xB506) ---
translator(0xB50E) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte result = r.getByte()
        if (result == 1) {
            byte itemCount = r.getByte()
            def items = (0..<itemCount).collect {
                ConsignmentEvent.ConsignmentItem.builder().personalId(r.getInt()).saleStatus(r.getByte()).refItemId(r.getInt()).sellCount(r.getInt()).price(r.getLong()).deposit(r.getLong()).sellFee(r.getLong()).endDate(r.getInt()).build()
            }
            return singleEvent(ConsignmentEvent.builder().fullName(machine).timestamp(ts).type(ConsignmentEvent.ConsignmentEventType.LIST).items(items).build())
        } else {
            return singleEvent(ConsignmentEvent.builder().fullName(machine).timestamp(ts).type(ConsignmentEvent.ConsignmentEventType.LIST).errorCode((int) r.getShort()).build())
        }
    } catch (Exception e) { return noEvents() }
}

translator(0xB508) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte result = r.getByte()
        if (result == 1) {
            byte itemCount = r.getByte()
            def items = (0..<itemCount).collect {
                ConsignmentEvent.ConsignmentItem.builder().sourceSlot(r.getByte()).saleStatus(r.getByte()).personalId(r.getInt()).refItemId(r.getInt()).deposit(r.getLong()).sellFee(r.getLong()).endDate(r.getInt()).build()
            }
            return singleEvent(ConsignmentEvent.builder().fullName(machine).timestamp(ts).type(ConsignmentEvent.ConsignmentEventType.REGISTERED).items(items).build())
        } else {
            return singleEvent(ConsignmentEvent.builder().fullName(machine).timestamp(ts).type(ConsignmentEvent.ConsignmentEventType.REGISTERED).errorCode((int) r.getShort()).build())
        }
    } catch (Exception e) { return noEvents() }
}

translator(0xB50A) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte result = r.getByte()
        if (result == 1) {
            return singleEvent(ConsignmentEvent.builder().fullName(machine).timestamp(ts).type(ConsignmentEvent.ConsignmentEventType.BOUGHT).personalId(r.getInt()).slot(r.getByte()).build())
        } else {
            return singleEvent(ConsignmentEvent.builder().fullName(machine).timestamp(ts).type(ConsignmentEvent.ConsignmentEventType.BOUGHT).errorCode((int) r.getShort()).build())
        }
    } catch (Exception e) { return noEvents() }
}

translator(0x350D) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte itemCount = r.getByte()
        def items = (0..<itemCount).collect {
            ConsignmentEvent.ConsignmentItem.builder().personalId(r.getInt()).refItemId(r.getInt()).saleStatus(r.getByte()).endDate(r.getInt()).build()
        }
        return singleEvent(ConsignmentEvent.builder().fullName(machine).timestamp(ts).type(ConsignmentEvent.ConsignmentEventType.UPDATED).items(items).build())
    } catch (Exception e) { return noEvents() }
}

translator(0xB50C) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte result = r.getByte()
        byte entryCount = 0, pageCount = 0
        def entries = []
        Integer errorCode = null
        if (result == 1) {
            entryCount = r.getByte()
            pageCount = r.getByte()
            entryCount.times {
                entries << ConsignmentSearchEvent.ConsignmentSearchEntry.builder().personalId(r.getInt()).sellerName(r.getString()).saleStatus(r.getByte()).refItemId(r.getInt()).sellCount(r.getInt()).price(r.getLong()).regDate(r.getInt()).build()
            }
        } else if (result == 2) {
            errorCode = (int) r.getShort() & 0xFFFF
        }
        return singleEvent(ConsignmentSearchEvent.builder().fullName(machine).timestamp(ts).result(result).entryCount(entryCount).pageCount(pageCount).entries(entries).errorCode(errorCode).build())
    } catch (Exception e) { return noEvents() }
}

translator(0xB506) { machine, packet ->
    try {
        def r = packet.streamReader
        long ts = System.currentTimeMillis()
        byte result = r.getByte()
        byte detailType = 0
        String sellerName = null
        int personalId = 0
        ConsignmentDetailEvent.ConsignmentItemDetail item = null
        Integer errorCode = null
        if (result == 1) {
            detailType = r.getByte()
            sellerName = r.getString()
            personalId = r.getInt()
            int refItemId = r.getInt()
            byte plus = r.getByte()
            long variance = r.getLong()
            int quantity = r.getInt()
            int durability = r.getInt()
            byte magicOptionCount = r.getByte()
            def magicOptions = (0..<magicOptionCount).collect {
                ConsignmentDetailEvent.MagicOption.builder().type(r.getInt()).value(r.getInt()).build()
            }
            item = ConsignmentDetailEvent.ConsignmentItemDetail.builder().refItemId(refItemId).plus(plus).variance(variance).quantity(quantity).durability(durability).magicOptions(magicOptions).build()
        } else if (result == 2) {
            errorCode = (int) r.getShort() & 0xFFFF
        }
        return singleEvent(ConsignmentDetailEvent.builder().fullName(machine).timestamp(ts).result(result).detailType(detailType).sellerName(sellerName).personalId(personalId).item(item).errorCode(errorCode).build())
    } catch (Exception e) { return noEvents() }
}
