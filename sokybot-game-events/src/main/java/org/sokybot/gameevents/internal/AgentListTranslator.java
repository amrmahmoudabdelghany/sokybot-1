package org.sokybot.gameevents.internal;

import org.osgi.service.component.annotations.Component;
import org.sokybot.api.events.AgentListEvent;
import org.sokybot.api.events.IGameEvent;
import org.sokybot.api.events.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Translates agent list packets (opcode 0xA101) to AgentListEvent.
 * Based on ServerOpcode.AGENT_LIST - received during login process.
 */
@Component(service = IPacketTranslator.class)
public class AgentListTranslator implements IPacketTranslator {
    
    private static final int AGENT_LIST_OPCODE = 0xA101;
    
    @Override
    public int getOpcode() {
        return AGENT_LIST_OPCODE;
    }
    
    @Override
    public IGameEvent translate(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            
            byte agentCount = reader.getByte();
            
            return new AgentListEvent(machineFullName, agentCount);
            
        } catch (Exception e) {
            return null;
        }
    }
}
