package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.job.JobJoinEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates job join packets (opcode 0xB0E1).
 * Based on RSBot JobJoinResponse.
 */
public class JobJoinTranslator extends AbstractTranslator {
    
    public JobJoinTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return 0xB0E1;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            IStreamReader reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            if (result != 1) {
                return noEvents();
            }
            byte jobType = reader.getByte();
            byte jobLevel = reader.getByte();
            long jobExp = Integer.toUnsignedLong(reader.getInt()); // RSBot ReadUInt -> long in Java
            // In RSBot: Experience = packet.ReadUInt()
            return singleEvent(new JobJoinEvent(machineFullName, jobType, jobLevel, jobExp));
        } catch (Exception e) {
            return noEvents();
        }
}
}
