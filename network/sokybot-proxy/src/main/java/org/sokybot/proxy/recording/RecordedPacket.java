package org.sokybot.proxy.recording;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable record of a captured packet.
 */
public class RecordedPacket {

    private final Instant timestamp;
    private final PacketDirection direction;
    private final int opcode;
    private final byte[] data;

    public RecordedPacket(Instant timestamp, PacketDirection direction, int opcode, byte[] data) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.direction = Objects.requireNonNull(direction, "direction must not be null");
        this.opcode = opcode;
        this.data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];
    }

    /**
     * Create a recorded packet with current timestamp.
     */
    public static RecordedPacket now(PacketDirection direction, int opcode, byte[] data) {
        return new RecordedPacket(Instant.now(), direction, opcode, data);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public PacketDirection getDirection() {
        return direction;
    }

    public int getOpcode() {
        return opcode;
    }

    /**
     * Get a copy of the packet data.
     */
    public byte[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    /**
     * Get packet data length.
     */
    public int getDataLength() {
        return data.length;
    }

    @Override
    public String toString() {
        return String.format("RecordedPacket{%s, opcode=0x%04X, len=%d}",
                direction, opcode, data.length);
    }
}
