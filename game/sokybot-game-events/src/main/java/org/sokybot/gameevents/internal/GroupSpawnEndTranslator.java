package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.entity.GroupSpawnEndEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates group spawn end packets (opcode 0x3018).
 * This signals the end of a batch spawn operation.
 * Based on RSBot EntityGroupSpawnEndResponse.
 */
public class GroupSpawnEndTranslator extends AbstractTranslator {
    
    public GroupSpawnEndTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0x3018;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        // Group spawn end signals completion of batch
        // The actual spawn parsing is handled by chunked packet mechanism
        return singleEvent(new GroupSpawnEndEvent(machineFullName));
}

}
