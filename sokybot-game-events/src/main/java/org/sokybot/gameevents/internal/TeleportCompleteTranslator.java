package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.teleport.TeleportCompleteEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates game reset complete packets (opcode 0x34B5).
 * This is sent after teleporting when the game state is fully loaded.
 * Based on RSBot GameResetCompleteResponse.
 */
public class TeleportCompleteTranslator extends AbstractTranslator {
    
    public TeleportCompleteTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return 0x34B5;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        // Game reset complete packet - no payload to parse
        return singleEvent(new TeleportCompleteEvent(machineFullName));
    }
}
