package org.sokybot.gameevents.internal;

import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.combat.AgentListEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
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
    protected List<IGameEvent> translateInternal(String machineFullName, ImmutablePacket packet) {
        try {
            var reader = packet.getStreamReader();
            java.util.List<org.sokybot.gameevents.dto.AgentServer> resList = new java.util.ArrayList<>();
            
            byte hasEntity = reader.getByte();
            if (hasEntity == 0x01) {
                reader.getByte();
                short farmSize = reader.getShort();
                // String farmName = new String(reader.getBytes(farmSize)); // Unused in event currently
                reader.getBytes(farmSize); // Skip farmName
                reader.getByte(); // spirator
                hasEntity = reader.getByte();
                while (hasEntity == 0x01) {
                    org.sokybot.gameevents.dto.AgentServer agent = org.sokybot.gameevents.dto.AgentServer.builder()
                            .serverId(reader.getShort())
                            .serverName(new String(reader.getBytes(reader.getShort())))
                            .onlineUsers(reader.getShort())
                            .maxUsers(reader.getShort())
                            .operating(reader.getByte())
                            .build();
                    reader.getByte(); // fram id
                    hasEntity = reader.getByte();
                    resList.add(agent);
                }
            }
            return singleEvent(new AgentListEvent(machineFullName, (byte)resList.size()));
        } catch (Exception e) {
            return noEvents();
        }
    }
}
