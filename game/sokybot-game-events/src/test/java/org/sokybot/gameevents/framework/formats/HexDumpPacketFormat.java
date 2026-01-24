package org.sokybot.gameevents.framework.formats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sokybot.gameevents.framework.PacketDataLoader;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Hex dump packet format loader.
 * Parses hexadecimal text files containing packet data.
 * 
 * <p>Supports multiple hex dump formats:
 * <ul>
 *   <li>Plain hex string: "A1 03 01 00 00 00"</li>
 *   <li>Hex dump with offsets: "0000: A1 03 01 00 00 00"</li>
 *   <li>Hex dump with ASCII: "0000: A1 03 01 00 00 00  |...."</li>
 *   <li>Multiple lines: Each line contains hex bytes</li>
 * </ul>
 * 
 * <p>Example hex dump file:
 * <pre>
 * A1 03 01
 * </pre>
 * 
 * or:
 * <pre>
 * 0000: A1 03 01 00 00 00  |......
 * </pre>
 */
public class HexDumpPacketFormat implements PacketDataLoader {
    
    // Pattern to match hex bytes: "A1", "03", etc.
    private static final Pattern HEX_BYTE_PATTERN = Pattern.compile("\\b([0-9A-Fa-f]{2})\\b");
    
    // Pattern to match offset prefix: "0000:", "0010:", etc.
    private static final Pattern OFFSET_PATTERN = Pattern.compile("^[0-9A-Fa-f]+:\\s*");
    
    @Override
    public ImmutablePacket loadPacket(InputStream source) throws IOException {
        List<Byte> bytes = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(source, StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Skip comment lines (starting with # or //)
                line = line.trim();
                if (line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                
                // Remove offset prefix if present (e.g., "0000: ")
                line = OFFSET_PATTERN.matcher(line).replaceFirst("");
                
                // Remove ASCII representation if present (e.g., " |....")
                int asciiIndex = line.indexOf('|');
                if (asciiIndex >= 0) {
                    line = line.substring(0, asciiIndex).trim();
                }
                
                // Extract hex bytes from line
                Matcher matcher = HEX_BYTE_PATTERN.matcher(line);
                while (matcher.find()) {
                    String hexByte = matcher.group(1);
                    byte value = (byte) Integer.parseInt(hexByte, 16);
                    bytes.add(value);
                }
            }
        }
        
        if (bytes.isEmpty()) {
            throw new IOException("No hex data found in input stream");
        }
        
        // Convert List<Byte> to byte[]
        byte[] packetData = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            packetData[i] = bytes.get(i);
        }
        
        return ImmutablePacket.wrap(packetData);
    }
    
    @Override
    public String getFormatName() {
        return "hexdump";
    }
    
    /**
     * Parse a hex string directly (useful for inline test data).
     * 
     * @param hexString Hex string with or without spaces (e.g., "A103" or "A1 03")
     * @return ImmutablePacket instance
     */
    public static ImmutablePacket parseHexString(String hexString) {
        // Remove all whitespace
        hexString = hexString.replaceAll("\\s+", "");
        
        // Must have even number of hex digits
        if (hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even number of digits: " + hexString);
        }
        
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            String hexByte = hexString.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(hexByte, 16);
        }
        
        return ImmutablePacket.wrap(bytes);
    }
}
