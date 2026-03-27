import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.dto.AgentInfo
import org.sokybot.gameevents.events.session.*
import org.sokybot.gameevents.events.combat.AgentListEvent

def parseAgentListWithLayout = { r, skipDividerAfterFarm ->
    def agents = []
    def count = 0
    def farmName = ""
    if (r.getByte() != 0x01) {
        return [agents: agents, count: 0, farmName: farmName]
    }
    r.getByte()
    short farmSize = r.getShort()
    farmName = new String(r.getBytes(farmSize))
    if (!skipDividerAfterFarm) {
        r.getByte()
    }
    byte hasEntity = r.getByte()
    while (hasEntity == 0x01) {
        short agentId = r.getShort()
        short agentNameLen = r.getShort()
        String agentName = new String(r.getBytes(agentNameLen))
        short onlineCount = r.getShort()
        short capacity = r.getShort()
        byte status = r.getByte()
        r.getByte()
        hasEntity = r.getByte()
        agents.add(new AgentInfo(agentId, agentName, onlineCount, capacity, status))
        count++
    }
    return [agents: agents, count: count, farmName: farmName]
}

def parseAgentListEvent = { machine, packet ->
    def first = parseAgentListWithLayout(packet.streamReader, false)
    if (!first.agents.isEmpty()) {
        return new AgentListEvent(machine, (byte) first.count, first.farmName, first.agents)
    }
    def second = parseAgentListWithLayout(packet.streamReader, true)
    return new AgentListEvent(machine, (byte) second.count, second.farmName, second.agents)
}

// Login request (0x6102) - client packet, may be used for logging
translator(0x6102) { machine, packet ->
    try {
        def r = packet.streamReader
        r.getByte()
        String username = PacketReaderUtils.readString(r)
        String password = PacketReaderUtils.readString(r)
        return singleEvent(new LoginRequestEvent(machine, username, password))
    } catch (Exception e) { return noEvents() }
}

// Agent list (0xA101) — ServerOpcode.AGENT_LIST / AgentListEvent
translator(0xA101) { machine, packet ->
    try {
        return singleEvent(parseAgentListEvent(machine, packet))
    } catch (Exception e) { return noEvents() }
}

// Login response (0xA102)
translator(0xA102) { machine, packet ->
    try {
        def r = packet.streamReader
        byte resultCode = r.getByte()
        boolean success = (resultCode == 0x01)
        if (success) {
            int loginId = r.getInt()
            String agentHost = r.getString()
            int agentPort = r.getShort() & 0xFFFF
            return singleEvent(new LoginResponseEvent(machine, success, resultCode, loginId, agentHost, agentPort))
        }
        return singleEvent(new LoginResponseEvent(machine, success, resultCode))
    } catch (Exception e) { return noEvents() }
}

// Gateway auth response (0xA103) — ServerOpcode.AUTH_RESPONSE
translator(0xA103) { machine, packet ->
    try {
        def r = packet.streamReader
        byte resultCode = r.getByte()
        boolean success = (resultCode != 0x02)
        return singleEvent(new AuthResponseEvent(machine, success, resultCode))
    } catch (Exception e) { return noEvents() }
}
