package org.sokybot.gameevents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chunked packet accumulation for multi-packet transactions.
 * Thread-safe implementation using ConcurrentHashMap keyed by BEGIN opcode.
 * Supports interleaved streams (e.g., CharacterData and GroupSpawn simultaneously).
 */
public class ChunkedPacketManager {
    
    /**
     * Active accumulators keyed by BEGIN opcode.
     */
    private final Map<Integer, ByteArrayOutputStream> accumulators = new ConcurrentHashMap<>();
    
    /**
     * Maps CHUNK opcode to its corresponding BEGIN opcode.
     * This allows chunk packets to be routed to the correct accumulator.
     */
    private static final Map<Integer, Integer> CHUNK_TO_BEGIN = Map.of(
        0x3013, 0x34A5,  // CharacterData → CharacterDataBegin
        0x3019, 0x3017   // GroupSpawn → GroupSpawnBegin
    );
    
    /**
     * Start a new chunked packet transaction.
     * 
     * @param beginOpcode The BEGIN opcode (e.g., 0x34A5 for CharacterDataBegin)
     */
    public void begin(int beginOpcode) {
        accumulators.put(beginOpcode, new ByteArrayOutputStream());
    }
    
    /**
     * Append data to an active chunked packet transaction.
     * 
     * @param chunkOpcode The CHUNK opcode (e.g., 0x3013 for CharacterData)
     * @param data The raw packet bytes to append
     */
    public void appendChunk(int chunkOpcode, byte[] data) {
        Integer beginOpcode = CHUNK_TO_BEGIN.get(chunkOpcode);
        if (beginOpcode != null) {
            ByteArrayOutputStream buffer = accumulators.get(beginOpcode);
            if (buffer != null) {
                try {
                    buffer.write(data);
                } catch (IOException e) {
                    // ByteArrayOutputStream doesn't throw IOException
                }
            }
        }
    }
    
    /**
     * Complete a chunked packet transaction and return accumulated data.
     * 
     * @param beginOpcode The BEGIN opcode to complete
     * @return The accumulated bytes, or empty array if no transaction was active
     */
    public byte[] complete(int beginOpcode) {
        ByteArrayOutputStream buffer = accumulators.remove(beginOpcode);
        return buffer != null ? buffer.toByteArray() : new byte[0];
    }
    
    /**
     * Check if a transaction is active for the given BEGIN opcode.
     */
    public boolean isActive(int beginOpcode) {
        return accumulators.containsKey(beginOpcode);
    }
    
    /**
     * Get the BEGIN opcode for a given CHUNK opcode.
     * 
     * @param chunkOpcode The chunk opcode
     * @return The corresponding BEGIN opcode, or null if not a chunk opcode
     */
    public static Integer getBeginOpcode(int chunkOpcode) {
        return CHUNK_TO_BEGIN.get(chunkOpcode);
    }
}
