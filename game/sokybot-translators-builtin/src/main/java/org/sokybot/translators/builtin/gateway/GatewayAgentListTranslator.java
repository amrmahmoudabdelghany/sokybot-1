package org.sokybot.translators.builtin.gateway;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.sokybot.gameevents.dto.AgentInfo;
import org.sokybot.gameevents.ChunkedPacketManager;
import org.sokybot.gameevents.events.combat.AgentListEvent;
import org.sokybot.gameevents.events.core.IGameEvent;
import org.sokybot.gameevents.events.core.IPacketTranslator;
import org.sokybot.network.packet.ImmutablePacket;

/**
 * Built-in translator for gateway AGENT_LIST (0xA101).
 * <p>
 * Tries three intro shapes (01+unk+farm, 01+farm, farm-at-offset-0), divider and UTF-8/UTF-16 variants,
 * and prefers parses that consume the entire payload.
 */
public enum GatewayAgentListTranslator implements IPacketTranslator {
    INSTANCE;

    private static final int OPCODE = 0xA101;
    /** 0 = 0x01, unk byte, farm… — 1 = 0x01, farm… — 2 = farm len at payload start */
    private static final int INTRO_COUNT = 3;

    @Override
    public int getOpcode() {
        return OPCODE;
    }

    @Override
    public List<IGameEvent> translate(String machineFullName, ImmutablePacket packet,
            ChunkedPacketManager chunkManager) {
        byte[] payload;
        try {
            payload = packet.getPacketReader().readFully();
        } catch (Exception e) {
            return List.of();
        }
        AgentListEvent evt = parse(machineFullName, payload);
        return evt != null ? List.of(evt) : List.of();
    }

    /**
     * Parses raw 0xA101 body (six-byte silkroad header already stripped — same as
     * {@link org.sokybot.network.packet.IPacketReader#readFully()}).
     */
    public static AgentListEvent parse(String machineFullName, byte[] payload) {
        if (payload == null || payload.length < 2) {
            return null;
        }
        AgentListEvent best = null;
        int bestScore = Integer.MIN_VALUE;
        boolean[] bools = { false, true };

        for (int intro = 0; intro < INTRO_COUNT; intro++) {
            for (boolean skipDividerAfterFarm : bools) {
                for (boolean unicodeFarm : bools) {
                    for (boolean unicodeAgents : bools) {
                        try {
                            ParseOutcome o = parseOneLayout(payload, intro, skipDividerAfterFarm, unicodeFarm,
                                    unicodeAgents);
                            if (o != null && !o.agents.isEmpty()) {
                                int score = o.consumed == payload.length ? 10000 : o.consumed;
                                score += o.agents.size();
                                if (score > bestScore) {
                                    bestScore = score;
                                    best = new AgentListEvent(machineFullName, (byte) o.agents.size(), o.farmName,
                                            o.agents);
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        return best;
    }

    private static final class ParseOutcome {
        final String farmName;
        final List<AgentInfo> agents;
        final int consumed;

        ParseOutcome(String farmName, List<AgentInfo> agents, int consumed) {
            this.farmName = farmName;
            this.agents = agents;
            this.consumed = consumed;
        }
    }

    private static ParseOutcome parseOneLayout(byte[] p, int introType, boolean skipDividerAfterFarm,
            boolean unicodeFarm, boolean unicodeAgents) {
        int pos = 0;
        if (introType == 0) {
            if (pos >= p.length || (p[pos++] & 0xFF) != 0x01) {
                return null;
            }
            if (pos >= p.length) {
                return null;
            }
            pos++;
        } else if (introType == 1) {
            if (pos >= p.length || (p[pos++] & 0xFF) != 0x01) {
                return null;
            }
        }
        if (pos + 2 > p.length) {
            return null;
        }
        int farmLen = u16(p, pos);
        if (farmLen < 0 || farmLen > 2048) {
            return null;
        }
        pos += 2;
        int farmBytes = unicodeFarm ? farmLen * 2 : farmLen;
        if (pos + farmBytes > p.length) {
            return null;
        }
        String farmName = unicodeFarm
                ? new String(p, pos, farmBytes, StandardCharsets.UTF_16LE)
                : new String(p, pos, farmBytes, StandardCharsets.UTF_8);
        pos += farmBytes;
        if (!skipDividerAfterFarm) {
            if (pos >= p.length) {
                return null;
            }
            pos++;
        }
        if (pos >= p.length) {
            return null;
        }
        int hasEntity = p[pos++] & 0xFF;
        List<AgentInfo> agents = new ArrayList<>();
        while (hasEntity == 0x01) {
            if (pos + 2 > p.length) {
                return null;
            }
            int agentId = u16(p, pos);
            pos += 2;
            if (pos + 2 > p.length) {
                return null;
            }
            int nameLen = u16(p, pos);
            if (nameLen < 0 || nameLen > 2048) {
                return null;
            }
            pos += 2;
            int nameBytes = unicodeAgents ? nameLen * 2 : nameLen;
            if (pos + nameBytes > p.length) {
                return null;
            }
            String name = unicodeAgents
                    ? new String(p, pos, nameBytes, StandardCharsets.UTF_16LE)
                    : new String(p, pos, nameBytes, StandardCharsets.UTF_8);
            pos += nameBytes;
            if (pos + 6 > p.length) {
                return null;
            }
            int online = u16(p, pos);
            pos += 2;
            int cap = u16(p, pos);
            pos += 2;
            byte status = p[pos++];
            pos++;
            hasEntity = p[pos++] & 0xFF;
            agents.add(new AgentInfo((short) agentId, name, (short) online, (short) cap, status));
        }
        return new ParseOutcome(farmName, agents, pos);
    }

    private static int u16(byte[] b, int i) {
        return (b[i] & 0xFF) | ((b[i + 1] & 0xFF) << 8);
    }
}
