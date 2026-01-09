package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates character data CHUNK packets (opcode 0x3013).
 * Accumulates packet data into ChunkedPacketManager without emitting events.
 * The actual parsing and event emission happens in CharacterDataEndTranslator.
 */
public class CharacterDataChunkTranslator extends AbstractTranslator {
    
    private static final int CHAR_DATA_CHUNK_OPCODE = 0x3013;
    
    public CharacterDataChunkTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return CHAR_DATA_CHUNK_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            // Append chunk data to the accumulator
            if (chunkManager != null && chunkManager.isActive(getOpcode())) {
                chunkManager.appendChunk(getOpcode(), packet.toBytes());
            }
            
            // Don't emit events for intermediate chunks - wait for END packet
            return noEvents();
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
