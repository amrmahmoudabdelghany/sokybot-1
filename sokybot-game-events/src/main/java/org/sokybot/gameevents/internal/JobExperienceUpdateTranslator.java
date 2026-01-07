package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.JobExperienceUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates job experience update packets (opcode 0x30E6) to JobExperienceUpdateEvent.
 * Reference: RSBot JobUpdateExperienceResponse = 0x30E6
 */
public class JobExperienceUpdateTranslator extends AbstractTranslator {
    
    private static final int JOB_EXP_OPCODE = 0x30E6;
    
    public JobExperienceUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return JOB_EXP_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte jobType = reader.getByte();
            int jobLevel = reader.getUnsignedByte();
            long experience = reader.getInt() & 0xFFFFFFFFL;
            
            return singleEvent(new JobExperienceUpdateEvent(machineFullName, jobType, jobLevel, experience));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
