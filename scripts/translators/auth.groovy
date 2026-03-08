import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.events.session.*
import org.sokybot.gameevents.events.combat.AgentListEvent

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

// Agent list (0xA101)
translator(0xA101) { machine, packet ->
    try {
        def r = packet.streamReader
        def count = 0
        if (r.getByte() == 0x01) {
            r.getByte()
            short farmSize = r.getShort()
            r.getBytes(farmSize)
            r.getByte()
            byte hasEntity = r.getByte()
            while (hasEntity == 0x01) {
                r.getShort()
                r.getBytes(r.getShort())
                r.getShort()
                r.getShort()
                r.getByte()
                r.getByte()
                hasEntity = r.getByte()
                count++
            }
        }
        return singleEvent(new AgentListEvent(machine, (byte)count))
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

// Auth response (0xA103)
translator(0xA103) { machine, packet ->
    try {
        def r = packet.streamReader
        byte resultCode = r.getByte()
        boolean success = (resultCode != 0x02)
        return singleEvent(new AuthResponseEvent(machine, success, resultCode))
    } catch (Exception e) { return noEvents() }
}
