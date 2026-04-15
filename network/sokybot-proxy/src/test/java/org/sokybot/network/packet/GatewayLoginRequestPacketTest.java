package org.sokybot.network.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Documents gateway 0x6102 layout aligned with {@code DispatcherImpl.sendLoginRequest}:
 * locale + username + password + ushort agent id (vSRO-style tail).
 */
class GatewayLoginRequestPacketTest {

    @Test
    void loginRequest_tailIsTwoByteAgentIdLittleEndian() {
        byte locale = 22;
        String username = "admin";
        String password = "123123123";
        int agentId = 3;
        byte[] userBytes = username.getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = password.getBytes(StandardCharsets.UTF_8);
        int packetLen = 1 + 2 + userBytes.length + 2 + passBytes.length + 2;
        MutablePacket packet = MutablePacket.getBuilder(packetLen, ClientOpcode.LOGIN_REQUEST)
                .put(locale)
                .putShort((short) userBytes.length)
                .putBytes(userBytes)
                .putShort((short) passBytes.length)
                .putBytes(passBytes)
                .putShort((short) agentId)
                .build();
        byte[] raw = packet.unwrap();
        assertEquals(packetLen + 6, raw.length);
        int bodyOff = 6;
        assertEquals(22, raw[bodyOff] & 0xFF);
        int ulen = (raw[bodyOff + 1] & 0xFF) | ((raw[bodyOff + 2] & 0xFF) << 8);
        assertEquals(5, ulen);
        int expectedBodyLen = 1 + 2 + 5 + 2 + 9 + 2;
        assertEquals(expectedBodyLen, packetLen);
        int tail = bodyOff + packetLen - 2;
        assertEquals(3, raw[tail] & 0xFF);
        assertEquals(0, raw[tail + 1] & 0xFF);
    }
}
