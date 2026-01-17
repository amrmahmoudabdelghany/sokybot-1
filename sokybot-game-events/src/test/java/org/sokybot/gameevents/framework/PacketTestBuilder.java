package org.sokybot.gameevents.framework;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import org.sokybot.network.packet.ImmutablePacket;

/**
 * Fluent builder for creating test packets.
 * Uses little-endian byte order (Silkroad standard).
 */
public class PacketTestBuilder {
    
    private final ByteBuffer buffer;
    
    private PacketTestBuilder(int capacity) {
        this.buffer = ByteBuffer.allocate(capacity);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    /**
     * Create a new packet builder with the specified initial capacity.
     */
    public static PacketTestBuilder create(int capacity) {
        return new PacketTestBuilder(capacity);
    }
    
    /**
     * Create a new packet builder with default capacity (128 bytes).
     */
    public static PacketTestBuilder create() {
        return create(128);
    }
    
    /**
     * Put a byte.
     */
    public PacketTestBuilder putByte(byte value) {
        buffer.put(value);
        return this;
    }
    
    /**
     * Put a byte (unsigned, as int).
     */
    public PacketTestBuilder putByte(int value) {
        buffer.put((byte) (value & 0xFF));
        return this;
    }
    
    /**
     * Put a boolean (as byte: 1 = true, 0 = false).
     */
    public PacketTestBuilder putBoolean(boolean value) {
        buffer.put((byte) (value ? 1 : 0));
        return this;
    }
    
    /**
     * Put a short.
     */
    public PacketTestBuilder putShort(short value) {
        buffer.putShort(value);
        return this;
    }
    
    /**
     * Put a short (unsigned, as int).
     */
    public PacketTestBuilder putShort(int value) {
        buffer.putShort((short) (value & 0xFFFF));
        return this;
    }
    
    /**
     * Put an int.
     */
    public PacketTestBuilder putInt(int value) {
        buffer.putInt(value);
        return this;
    }
    
    /**
     * Put a long.
     */
    public PacketTestBuilder putLong(long value) {
        buffer.putLong(value);
        return this;
    }
    
    /**
     * Put a float.
     */
    public PacketTestBuilder putFloat(float value) {
        buffer.putFloat(value);
        return this;
    }
    
    /**
     * Put a string (UTF-8 encoded with 2-byte length prefix).
     */
    public PacketTestBuilder putString(String value) {
        if (value == null) {
            buffer.putShort((short) 0);
            return this;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) bytes.length);
        buffer.put(bytes);
        return this;
    }
    
    /**
     * Put raw bytes.
     */
    public PacketTestBuilder putBytes(byte[] bytes) {
        if (bytes != null) {
            buffer.put(bytes);
        }
        return this;
    }
    
    /**
     * Build the ImmutablePacket from the accumulated bytes.
     */
    public ImmutablePacket build() {
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return ImmutablePacket.wrap(data);
    }
    
    /**
     * Get the current size of the packet being built.
     */
    public int size() {
        return buffer.position();
    }
}
