import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.events.storage.*
import org.sokybot.network.packet.ImmutablePacket

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Storage warehouse translators (personal/guild open, data chunks, take ack).
 * 0x3049 chunk layout is shard-variable; parsing is defensive with graceful degradation.
 */

// Personal storage open
translator(0x3047) { machine, packet ->
    try {
        return singleEvent(new StorageOpenEvent(machine, packet.streamReader.getLong(), (byte) 0))
    } catch (Exception e) {
        return noEvents()
    }
}

// Guild storage open
translator(0x3253) { machine, packet ->
    try {
        return singleEvent(new StorageOpenEvent(machine, packet.streamReader.getLong(), (byte) 1))
    } catch (Exception e) {
        return noEvents()
    }
}

// Take-from-storage ack
translator(0xB558) { machine, packet ->
    try {
        def r = packet.streamReader
        boolean success = r.getBoolean()
        int itemCount = success ? r.getInt() : 0
        return singleEvent(new StorageBoxTakeItemEvent(machine, success, itemCount))
    } catch (Exception e) {
        return noEvents()
    }
}

// Storage data chunk: structured item rows + optional finalize when last chunk flag is set
translator(0x3049) { machine, packet ->
    try {
        byte[] payload = packet.packetReader.readFully()
        if (payload.length < 2) {
            return noEvents()
        }
        long ts = System.currentTimeMillis()
        byte storageType = (byte) 0

        boolean isLastFlag = (payload[1] & 0xFF) != 0

        // Fast path: header + exactly 8 byte tail (common finalize-only stub on some shards)
        if (isLastFlag && payload.length == 10) {
            ByteBuffer bb = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
            bb.get() // chunk idx
            bb.get() // is last
            int totalSlots = bb.getInt()
            int filledSlots = bb.getInt()
            return singleEvent(new StorageBoxFinalizeEvent(machine, ts, storageType, totalSlots, filledSlots))
        }

        ImmutablePacket p2 = ImmutablePacket.wrap(payload)
        def r = p2.streamReader
        r.getByte() // chunk index
        byte isLastRaw = r.getByte()
        boolean isLast = (isLastRaw & 0xFF) != 0

        List out = []
        while (true) {
            try {
                int[] pair = PacketReaderUtils.readInventoryStyleSlotAndRefId(r)
                int slot = pair[0]
                int refId = pair[1]
                if (refId == 0) {
                    continue
                }
                out.add(new StorageItemUpdateEvent(machine, ts, storageType, slot, refId, 1, -1, new byte[0]))
            } catch (Exception ignored) {
                break
            }
        }

        if (isLast) {
            int totalSlots = -1
            int filledSlots = -1
            try {
                totalSlots = r.getInt()
                filledSlots = r.getInt()
            } catch (Exception ignored) {
                // remain -1 when tail is absent or truncated
            }
            out.add(new StorageBoxFinalizeEvent(machine, ts, storageType, totalSlots, filledSlots))
        }

        return out
    } catch (Exception e) {
        return noEvents()
    }
}
