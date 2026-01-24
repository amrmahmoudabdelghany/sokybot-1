package org.sokybot.proxy.test.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;

/**
 * Utility class for creating test packets in proxy tests.
 */
public class PacketTestUtils {
    
    /**
     * Creates an ImmutablePacket for testing.
     * 
     * @param opcode The packet opcode
     * @param data The packet data (excluding header)
     * @return An ImmutablePacket instance
     */
    public ImmutablePacket createPacket(int opcode, byte[] data) {
        return createPacket(opcode, data, false, false);
    }
    
    /**
     * Creates an ImmutablePacket for testing with encoding flags.
     * 
     * @param opcode The packet opcode
     * @param data The packet data (excluding header)
     * @param packetEncrypted Whether the packet header is encrypted
     * @param dataEncrypted Whether the packet data is encrypted
     * @return An ImmutablePacket instance
     */
    public ImmutablePacket createPacket(int opcode, byte[] data, boolean packetEncrypted, boolean dataEncrypted) {
        int dataLength = data != null ? data.length : 0;
        ByteBuffer buffer = ByteBuffer.allocate(6 + dataLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // Packet size (with encryption flag)
        short size = (short) dataLength;
        if (packetEncrypted) {
            size |= 0x8000;
        }
        buffer.putShort(size);
        
        // Opcode
        buffer.putShort((short) opcode);
        
        // Count byte (default 0)
        buffer.put((byte) 0);
        
        // CRC byte (default 0)
        buffer.put((byte) 0);
        
        // Data
        if (data != null) {
            buffer.put(data);
        }
        
        buffer.flip();
        return ImmutablePacket.wrap(buffer.array());
    }
    
    /**
     * Creates a MutablePacket for sending in tests.
     * 
     * @param opcode The packet opcode
     * @param data The packet data (excluding header)
     * @return A MutablePacket instance
     */
    public MutablePacket createMutablePacket(int opcode, byte[] data) {
        int dataLength = data != null ? data.length : 0;
        ByteBuffer buffer = ByteBuffer.allocate(6 + dataLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        
        // Initialize header
        buffer.putShort((short) dataLength); // Size
        buffer.putShort((short) opcode);     // Opcode
        buffer.put((byte) 0);                // Count
        buffer.put((byte) 0);                // CRC
        
        // Data
        if (data != null) {
            buffer.put(data);
        }
        
        return MutablePacket.wrap(buffer.array());
    }
    
    /**
     * Creates a packet with string data.
     * 
     * @param opcode The packet opcode
     * @param stringData The string to include in the packet (UTF-8 encoded)
     * @return An ImmutablePacket instance
     */
    public ImmutablePacket createPacketWithString(int opcode, String stringData) {
        byte[] stringBytes = stringData != null ? stringData.getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0];
        
        // Add 2-byte length prefix for string
        ByteBuffer buffer = ByteBuffer.allocate(2 + stringBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) stringBytes.length);
        buffer.put(stringBytes);
        
        return createPacket(opcode, buffer.array());
    }
    
    /**
     * Creates a packet with integer data.
     * 
     * @param opcode The packet opcode
     * @param intValue The integer value
     * @return An ImmutablePacket instance
     */
    public ImmutablePacket createPacketWithInt(int opcode, int intValue) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(intValue);
        
        return createPacket(opcode, buffer.array());
    }
    
    /**
     * Creates a packet with short data.
     * 
     * @param opcode The packet opcode
     * @param shortValue The short value
     * @return An ImmutablePacket instance
     */
    public ImmutablePacket createPacketWithShort(int opcode, short shortValue) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(shortValue);
        
        return createPacket(opcode, buffer.array());
    }
    
    /**
     * Creates an empty packet (only header).
     * 
     * @param opcode The packet opcode
     * @return An ImmutablePacket instance
     */
    public ImmutablePacket createEmptyPacket(int opcode) {
        return createPacket(opcode, new byte[0]);
    }
}
