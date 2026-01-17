package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.character.CharacterDataBeginEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates character data BEGIN packets (opcode 0x34A5).
 * Signals the start of a chunked character data transaction.
 * Initializes the ChunkedPacketManager accumulator for this transaction.
 */
public class CharacterDataBeginTranslator extends AbstractTranslator {
    
    private static final int CHAR_DATA_BEGIN_OPCODE = 0x34A5;
    public CharacterDataBeginTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return CHAR_DATA_BEGIN_OPCODE;
    }
    
    @Override
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            // Start accumulating data for this transaction
            ChunkedPacketManager chunkManager = getChunkManager(machineFullName);
            if (chunkManager != null) {
                chunkManager.begin(CHAR_DATA_BEGIN_OPCODE);
            }
            
            // Emit event to signal transaction start
            return singleEvent(new CharacterDataBeginEvent(machineFullName));
        } catch (Exception e) {
            return noEvents();
        }
    }
