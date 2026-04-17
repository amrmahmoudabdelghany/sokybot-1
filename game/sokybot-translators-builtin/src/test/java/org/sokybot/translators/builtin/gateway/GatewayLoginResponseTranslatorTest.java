package org.sokybot.translators.builtin.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.session.LoginResponseEvent;
import org.sokybot.network.packet.ImmutablePacket;

class GatewayLoginResponseTranslatorTest {

    /** Full packet as captured: 8 bytes incl. 6-byte header + body 02 0D (failure + code 13). */
    @Test
    void translate_failure_02_0D_producesLoginResponseEvent() {
        byte[] raw = new byte[] {
                0x02, 0x00, 0x02, (byte) 0xA1, 0x00, 0x00, 0x02, 0x0D,
        };
        ImmutablePacket p = ImmutablePacket.wrap(raw, org.sokybot.network.packet.Encoding.PLAIN,
                org.sokybot.network.NetworkPeer.SERVER);
        List<IGameEvent> evs = GatewayLoginResponseTranslator.INSTANCE.translate("g.test", p, null);
        assertThat(evs).hasSize(1);
        assertThat(evs.get(0)).isInstanceOf(LoginResponseEvent.class);
        LoginResponseEvent lr = (LoginResponseEvent) evs.get(0);
        assertThat(lr.isSuccess()).isFalse();
        assertThat(lr.getResultCode() & 0xFF).isEqualTo(0x0D);
    }
}
