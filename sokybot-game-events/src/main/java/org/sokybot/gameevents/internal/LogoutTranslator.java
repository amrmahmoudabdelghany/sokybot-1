package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.session.LogoutEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates logout success packets (opcode 0x300A).
 * Based on RSBot LogoutSuccessResponse.
 */
public class LogoutTranslator extends AbstractTranslator {
    
    public LogoutTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return 0x300A;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        // Logout packet has no payload to parse
        return singleEvent(new LogoutEvent(machineFullName));
    }
}
