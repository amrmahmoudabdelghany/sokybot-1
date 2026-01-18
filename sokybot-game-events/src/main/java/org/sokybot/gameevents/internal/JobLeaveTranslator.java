package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.job.JobLeaveEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates job leave packets (opcode 0xB0E2).
 * Based on RSBot JobLeaveResponse.
 */
public class JobLeaveTranslator extends AbstractTranslator {
    
    public JobLeaveTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0xB0E2;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            if (result != 1) {
                return noEvents();
            }
            return singleEvent(new JobLeaveEvent(machineFullName));
        } catch (Exception e) {
            return noEvents();
        }
}
}
