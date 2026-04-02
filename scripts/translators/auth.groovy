import java.nio.charset.StandardCharsets

import org.sokybot.gameevents.script.PacketReaderUtils
import org.sokybot.gameevents.dto.AgentInfo
import org.sokybot.gameevents.events.session.*
import org.sokybot.gameevents.events.combat.AgentListEvent

/**
 * Gateway 0xA101 layout differs by client flavor:
 * - Many private / vSRO builds use UTF-16LE agent names (ushort = wchar count, see party.groovy 0x306E).
 * - Older/iSRO-style often uses single-byte names (ushort = byte length).
 * Try multiple combinations so LoginState receives a non-empty agent list.
 */
def parseAgentListWithLayout = { r, skipDividerAfterFarm, unicodeFarm, unicodeAgents ->
    def agents = []
    def count = 0
    def farmName = ""
    if (r.getByte() != 0x01) {
        return [agents: agents, count: 0, farmName: farmName]
    }
    r.getByte()
    short farmSize = r.getShort()
    int fl = farmSize & 0xFFFF
    farmName = unicodeFarm ? r.getUnicodeString(fl) : new String(r.getBytes(fl), StandardCharsets.UTF_8)
    if (!skipDividerAfterFarm) {
        r.getByte()
    }
    byte hasEntity = r.getByte()
    while (hasEntity == 0x01) {
        short agentId = r.getShort()
        short agentNameLen = r.getShort()
        int nl = agentNameLen & 0xFFFF
        String agentName = unicodeAgents ? r.getUnicodeString(nl) : new String(r.getBytes(nl), StandardCharsets.UTF_8)
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
    def skipOpts = [false, true]
    // Prefer Unicode agent names first (typical vSRO/private gateway); then ANSI (older iSRO-style).
    def farmOpts = [false, true]
    def agentOpts = [true, false]
    def best = null
    for (def skipDiv : skipOpts) {
        for (def uniFarm : farmOpts) {
            for (def uniAgent : agentOpts) {
                try {
                    def parsed = parseAgentListWithLayout(packet.streamReader, skipDiv, uniFarm, uniAgent)
                    if (parsed.agents != null && !parsed.agents.isEmpty()) {
                        return new AgentListEvent(machine, (byte) parsed.count, parsed.farmName, parsed.agents)
                    }
                    if (parsed != null && best == null) {
                        best = parsed
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }
    if (best != null) {
        return new AgentListEvent(machine, (byte) (best.count ?: 0), String.valueOf(best.farmName ?: ""), best.agents ?: [])
    }
    return null
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
        def ev = parseAgentListEvent(machine, packet)
        if (ev == null) {
            return noEvents()
        }
        return singleEvent(ev)
    } catch (Exception e) { return noEvents() }
}

// Login response (0xA102)
translator(0xA102) { machine, packet ->
    try {
        def r = packet.streamReader
        byte status = r.getByte()
        boolean success = (status == 0x01)
        if (success) {
            int loginId = r.getInt()
            String agentHost = r.getString()
            int agentPort = r.getShort() & 0xFFFF
            return singleEvent(new LoginResponseEvent(machine, true, status, loginId, agentHost, agentPort))
        }
        byte errorCode = status
        try {
            // Failure payloads vary across server builds; consume optional error byte only when present.
            errorCode = r.getByte()
        } catch (Exception ignored) {
            // Keep fallback to status byte (0x02) for malformed/short payloads.
        }
        return singleEvent(new LoginResponseEvent(machine, false, errorCode))
    } catch (Exception e) {
        // Never swallow failed login responses due to parser shape mismatch.
        return singleEvent(new LoginResponseEvent(machine, false, (byte) 0x00))
    }
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

// Passcode request (0x6105) - some SRO variants request second-factor/passcode.
translator(0x6105) { machine, packet ->
    return singleEvent(new PasscodeRequiredEvent(machine))
}

// Captcha challenge (0x610C): optional [captchaId:int][size:short][bytes]
translator(0x610C) { machine, packet ->
    try {
        def r = packet.streamReader
        int captchaId = 0
        int size = 0
        byte[] payload = null
        try {
            captchaId = r.getInt()
        } catch (Exception ignored) {
            captchaId = 0
        }
        try {
            size = r.getShort() & 0xFFFF
        } catch (Exception ignored) {
            size = 0
        }
        if (size > 0 && size <= 262144) {
            payload = r.getBytes(size)
        }
        return singleEvent(new CaptchaChallengeEvent(machine, captchaId, payload))
    } catch (Exception e) {
        return noEvents()
    }
}
