package org.sokybot.behaviors.logistics.router;

import java.nio.charset.StandardCharsets;

import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

/**
 * Epic #24: placeholder Silk Road trade client opcodes (tune per shard).
 */
public final class RouterTradePackets {

    public static final int CLIENT_TRADE_REQUEST = 0x7081;
    public static final int CLIENT_TRADE_ADD_ITEM = 0x7082;
    public static final int CLIENT_TRADE_CONFIRM = 0x7083;

    private RouterTradePackets() {
    }

    /**
     * Trade request by target character name (placeholder payload).
     */
    public static void sendTradeRequest(IWorkflowContext ctx, String targetCharName) {
        if (ctx == null) {
            return;
        }
        String name = targetCharName != null ? targetCharName : "";
        byte[] u = name.getBytes(StandardCharsets.UTF_16LE);
        int body = 2 + u.length;
        MutablePacket packet = MutablePacket.getBuilder(body, CLIENT_TRADE_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putShort((short) (name.length() & 0xFFFF))
                .putBytes(u)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendTradeAddItem(IWorkflowContext ctx, byte slot) {
        if (ctx == null) {
            return;
        }
        MutablePacket packet = MutablePacket.getBuilder(1, CLIENT_TRADE_ADD_ITEM)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .put(slot)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendTradeConfirm(IWorkflowContext ctx) {
        if (ctx == null) {
            return;
        }
        MutablePacket packet = MutablePacket.getBuilder(0, CLIENT_TRADE_CONFIRM)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }
}
