package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.api.events.AgentListEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.gameevents.AbstractTranslator;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Translates agent list packets (opcode 0xA101) to AgentListEvent.
 * Based on ServerOpcode.AGENT_LIST - received during login process.
 */
public class AgentListTranslator extends AbstractTranslator {
    
    private static final int AGENT_LIST_OPCODE = 0xA101;
    
    public AgentListTranslator(IGameDataLookup lookup) {
        super(lookup);
    }
    
    @Override
    public int getOpcode() {
        return AGENT_LIST_OPCODE;
    }
    
    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte agentCount = reader.getByte();
            
            return singleEvent(new AgentListEvent(machineFullName, agentCount));
            
        } catch (Exception e) {
            return noEvents();
        }
    }
}
