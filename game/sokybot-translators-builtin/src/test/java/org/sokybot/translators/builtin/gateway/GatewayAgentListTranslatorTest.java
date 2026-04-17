package org.sokybot.translators.builtin.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sokybot.gameevents.events.combat.AgentListEvent;

class GatewayAgentListTranslatorTest {

    @Test
    void parse_compactIntro_utf8_oneAgent_consumesPayload() {
        // introType 1: 0x01 then farm u16 + utf8 farm, no leading unk; skip divider after farm
        List<Byte> b = new ArrayList<>();
        b.add((byte) 0x01);
        String farm = "FarmX";
        putUtf8String(b, farm);
        // skipDividerAfterFarm: true — no byte after farm
        b.add((byte) 0x01); // hasEntity
        int agentId = 7;
        b.add((byte) (agentId & 0xff));
        b.add((byte) ((agentId >> 8) & 0xff));
        String agentName = "Agent1";
        putUtf8String(b, agentName);
        b.add((byte) 0); // online lo
        b.add((byte) 0); // online hi
        b.add((byte) 0); // cap lo
        b.add((byte) 0); // cap hi
        b.add((byte) 0); // status
        b.add((byte) 0); // padding/skip
        b.add((byte) 0); // hasEntity terminator

        byte[] payload = toArray(b);
        AgentListEvent evt = GatewayAgentListTranslator.parse("g.m1", payload);
        assertThat(evt).isNotNull();
        assertThat(evt.getFarmName()).isEqualTo(farm);
        assertThat(evt.getAgents()).hasSize(1);
        assertThat(evt.getAgents().get(0).getId()).isEqualTo((short) agentId);
        assertThat(evt.getAgents().get(0).getName()).isEqualTo(agentName);
    }

    @Test
    void parse_classicIntro_utf8_withDividerAndTerminator() {
        // introType 0: 0x01, unk, farm...
        List<Byte> b = new ArrayList<>();
        b.add((byte) 0x01);
        b.add((byte) 0x00); // unk
        String farm = "F";
        putUtf8String(b, farm);
        b.add((byte) 0x00); // divider when skipDividerAfterFarm false
        b.add((byte) 0x01); // hasEntity
        b.add((byte) 1);
        b.add((byte) 0); // agent id 1
        putUtf8String(b, "N");
        b.add((byte) 0);
        b.add((byte) 0);
        b.add((byte) 0);
        b.add((byte) 0);
        b.add((byte) 0); // status
        b.add((byte) 0); // padding
        b.add((byte) 0); // hasEntity terminator

        AgentListEvent evt = GatewayAgentListTranslator.parse("g.m1", toArray(b));
        assertThat(evt).isNotNull();
        assertThat(evt.getFarmName()).isEqualTo(farm);
        assertThat(evt.getAgents()).hasSize(1);
    }

    private static void putUtf8String(List<Byte> b, String s) {
        byte[] raw = s.getBytes(StandardCharsets.UTF_8);
        b.add((byte) (raw.length & 0xff));
        b.add((byte) ((raw.length >> 8) & 0xff));
        for (byte x : raw) {
            b.add(x);
        }
    }

    private static byte[] toArray(List<Byte> b) {
        byte[] out = new byte[b.size()];
        for (int i = 0; i < b.size(); i++) {
            out[i] = b.get(i);
        }
        return out;
    }
}
