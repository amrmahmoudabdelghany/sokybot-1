package org.sokybot.behaviors.party.internal;

import java.nio.charset.StandardCharsets;

import org.sokybot.commons.SilkroadUtils;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

/**
 * Client party opcodes and shared movement/skill helpers (mirrors {@code CombatPackets} layout).
 */
final class PartyPackets {

    /** Party invite by character name (RSBot/vSRO-style client opcode). */
    static final int CLIENT_PARTY_INVITE = 0x7060;

    /**
     * Party invite response / accept-decline (project uses the same opcode family as server notification
     * {@code 0x3080}; tune per shard if the client uses a different request opcode).
     */
    static final int CLIENT_PARTY_INVITE_RESPONSE = 0x3080;

    /** Party matching window / form submission. */
    static final int CLIENT_PARTY_MATCHING_FORM = 0x706C;

    private static final int CLIENT_SKILL_CAST = ClientOpcode.JOIN_REQUEST;
    private static final int CLIENT_CHAR_MOVEMENT = ClientOpcode.CHAR_MOVEMENT;

    private PartyPackets() {
    }

    static void sendInvite(IWorkflowContext ctx, String characterName) {
        if (ctx == null || characterName == null || characterName.isEmpty()) {
            return;
        }
        byte[] u = characterName.getBytes(StandardCharsets.UTF_16LE);
        int body = 2 + u.length;
        MutablePacket packet = MutablePacket.getBuilder(body, CLIENT_PARTY_INVITE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putShort((short) (characterName.length() & 0xFFFF))
                .putBytes(u)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendInviteResponse(IWorkflowContext ctx, boolean accept) {
        if (ctx == null) {
            return;
        }
        MutablePacket packet = MutablePacket.getBuilder(1, CLIENT_PARTY_INVITE_RESPONSE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(accept ? (byte) 1 : (byte) 0)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    /**
     * Registers or refreshes a party-matching advertisement (best-effort payload).
     */
    static void sendMatchingForm(IWorkflowContext ctx, String title, int minLevel, int maxLevel) {
        if (ctx == null) {
            return;
        }
        String t = title != null ? title : "";
        byte[] u = t.getBytes(StandardCharsets.UTF_16LE);
        int body = 1 + 2 + u.length + 1 + 1;
        MutablePacket packet = MutablePacket.getBuilder(body, CLIENT_PARTY_MATCHING_FORM)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x01)
                .putShort((short) (t.length() & 0xFFFF))
                .putBytes(u)
                .put((byte) (minLevel & 0xFF))
                .put((byte) (maxLevel & 0xFF))
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendSkillCast(IWorkflowContext ctx, int skillRefId, int targetUniqueId) {
        if (ctx == null) {
            return;
        }
        MutablePacket packet = MutablePacket.getBuilder(10, CLIENT_SKILL_CAST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) 0x01)
                .put((byte) 0x01)
                .putInt(skillRefId)
                .put((byte) 0x01)
                .putInt(targetUniqueId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    static void sendCharMove(IWorkflowContext ctx, float dstX, float dstY, float dstZ, int sectorX, int sectorY) {
        if (ctx == null) {
            return;
        }
        short sx = (short) sectorX;
        short sy = (short) sectorY;
        int xOff = SilkroadUtils.getXOffset((int) dstX, sx);
        int zOff = SilkroadUtils.getSectorOffset(dstZ);
        int yOff = SilkroadUtils.getYOffset((int) dstY, sy);
        MutablePacket packet = MutablePacket.getBuilder(8, CLIENT_CHAR_MOVEMENT)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put((byte) (sectorX & 0xFF))
                .put((byte) (sectorY & 0xFF))
                .putShort(clampToShort(xOff))
                .putShort(clampToShort(zOff))
                .putShort(clampToShort(yOff))
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    private static short clampToShort(int v) {
        if (v > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (v < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) v;
    }
}
