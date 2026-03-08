package org.sokybot.gameevents.events.combat;
import org.sokybot.gameevents.events.core.IGameEvent;

/**
 * Event fired when agent/server list is received.
 * Based on ServerOpcode.AGENT_LIST (0xA101).
 */
public class AgentListEvent implements IGameEvent {
    
    private final String fullName;
    private final long timestamp;
    private final byte agentCount;
    
    public AgentListEvent(String machineFullName, byte agentCount) {
        this.fullName = machineFullName;
        this.timestamp = System.currentTimeMillis();
        this.agentCount = agentCount;
    }
    
    @Override
    public String getFullName() { return fullName; }
    
    @Override
    public String getGroupName() { return fullName.split("\\.")[0]; }
    
    @Override
    public String getMachineName() { return fullName.split("\\.")[1]; }
    
    @Override
    public long getTimestamp() { return timestamp; }
    
    public byte getAgentCount() { return agentCount; }
}
