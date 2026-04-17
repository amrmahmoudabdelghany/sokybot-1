package org.sokybot.translators.builtin.gateway;

import java.util.List;

import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.gameevents.events.session.LoginResponseEvent;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Built-in translator for gateway LOGIN_RESPONSE (0xA102). Mirrors
 * {@code scripts/translators/auth.groovy} so game model dispatch always receives
 * login results via the runtime packet bridge when scripted translators are missing or fail.
 */
public enum GatewayLoginResponseTranslator implements IPacketTranslator {

    INSTANCE;

    private static final int OPCODE = 0xA102;

    @Override
    public int getOpcode() {
        return OPCODE;
    }

    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet,
            ChunkedPacketManager chunkManager) {
        try {
            IStreamReader r = packet.getStreamReader();
            byte status = r.getByte();
            if (status == 0x01) {
                int loginId = r.getInt();
                String agentHost = r.getString();
                int agentPort = r.getShort() & 0xFFFF;
                return List.of(new LoginResponseEvent(machineFullName, true, status, loginId, agentHost, agentPort));
            }
            byte errorCode = status;
            try {
                errorCode = r.getByte();
            } catch (Exception ignored) {
                // Single-byte or truncated failure payloads
            }
            return List.of(new LoginResponseEvent(machineFullName, false, errorCode));
        } catch (Exception e) {
            return List.of(new LoginResponseEvent(machineFullName, false, (byte) 0));
        }
    }
}
