package org.sokybot.behaviors.social.net;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

public final class SocialPackets {

    private static final int CLIENT_CHAT_REQUEST_OPCODE = 0x7025;
    private static final ConcurrentHashMap<String, AtomicInteger> chatIndices = new ConcurrentHashMap<>();

    private SocialPackets() {
        // Utility class
    }

    public static MutablePacket chatRequest(String machineId, byte channel, String message) {
        return chatRequest(machineId, channel, null, message);
    }

    public static MutablePacket chatRequest(String machineId, byte channel, String recipient, String message) {
        AtomicInteger indexCounter = chatIndices.computeIfAbsent(machineId, k -> new AtomicInteger(1));
        byte chatIndex = (byte) indexCounter.getAndIncrement();

        byte[] msgBytes = message.getBytes(StandardCharsets.US_ASCII);
        byte[] recBytes = recipient != null ? recipient.getBytes(StandardCharsets.US_ASCII) : new byte[0];
        
        int capacity = 2 + (recipient != null ? 2 + recBytes.length : 0) + 2 + msgBytes.length;

        var builder = MutablePacket.getBuilder(capacity, CLIENT_CHAT_REQUEST_OPCODE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .put(channel)
                .put(chatIndex);

        if (channel == 2 && recipient != null) { // Assuming 2 is PRIVATE channel
            builder.putShort((short) recBytes.length);
            builder.putBytes(recBytes);
        }

        builder.putShort((short) msgBytes.length);
        builder.putBytes(msgBytes);

        return builder.build();
    }
}
