package org.sokybot.gameevents.framework.formats;

import java.io.IOException;
import java.io.InputStream;

import org.sokybot.gameevents.framework.PacketDataLoader;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Binary packet format loader.
 * Reads raw binary packet data from input stream.
 * Useful for testing with real captured packets.
 */
public class BinaryPacketFormat implements PacketDataLoader {
    
    @Override
    public ImmutablePacket loadPacket(InputStream source) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = source.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return ImmutablePacket.wrap(buffer.toByteArray());
    }
    
    @Override
    public String getFormatName() {
        return "binary";
    }
}
