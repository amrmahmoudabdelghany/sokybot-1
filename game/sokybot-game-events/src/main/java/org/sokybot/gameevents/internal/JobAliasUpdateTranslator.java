package org.sokybot.gameevents.internal;

import java.util.List;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.job.JobAliasUpdateEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;
/**
 * Translates job alias update packets (opcode 0xB0E3) to JobAliasUpdateEvent.
 * Reference: RSBot JobAliasUpdateResponse = 0xB0E3
 */
public class JobAliasUpdateTranslator extends AbstractTranslator {
    
    private static final int JOB_ALIAS_OPCODE = 0xB0E3;
    public JobAliasUpdateTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    @Override
    public int getOpcode() {
        return JOB_ALIAS_OPCODE;
    }
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte result = reader.getByte();
            boolean success = result == 0x01;
            String jobAlias = "";
            if (success) {
                reader.getByte();  // isUpdate flag
                jobAlias = reader.getString();
            }
            return singleEvent(new JobAliasUpdateEvent(machineFullName, success, jobAlias));
        } catch (Exception e) {
            return noEvents();
        }
}
}
