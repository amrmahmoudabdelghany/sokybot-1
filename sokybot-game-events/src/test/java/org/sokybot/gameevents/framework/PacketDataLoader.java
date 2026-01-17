package org.sokybot.gameevents.framework;

import java.io.IOException;
import java.io.InputStream;

import org.sokybot.network.packet.ImmutablePacket;

/**
 * Interface for loading packet data from various formats.
 * Supports future real packet data testing with flexible format abstraction.
 */
public interface PacketDataLoader {
    
    /**
     * Load a packet from an input stream.
     * 
     * @param source The input stream containing packet data
     * @return An ImmutablePacket instance
     * @throws IOException If there's an error reading the packet data
     */
    ImmutablePacket loadPacket(InputStream source) throws IOException;
    
    /**
     * Load a packet from a resource path.
     * 
     * @param resourcePath Classpath resource path (e.g., "packets/auth/success_0xA103.bin")
     * @return An ImmutablePacket instance
     * @throws IOException If the resource cannot be found or read
     */
    default ImmutablePacket loadPacketFromResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return loadPacket(is);
        }
    }
    
    /**
     * Get the format name for this loader.
     * 
     * @return The format name (e.g., "binary", "hex", "json")
     */
    String getFormatName();
}
