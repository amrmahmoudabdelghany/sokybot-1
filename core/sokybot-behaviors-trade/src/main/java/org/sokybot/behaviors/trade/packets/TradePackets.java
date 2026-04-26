package org.sokybot.behaviors.trade.packets;

import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

/**
 * Outbound client opcodes for player-to-player exchange (vSRO-style). Body layouts follow common private-server
 * conventions; adjust if your shard differs.
 */
public final class TradePackets {

    public static final int CLIENT_EXCHANGE_REQUEST = ClientOpcode.EXCHANGE_REQUEST;
    public static final int CLIENT_EXCHANGE_CONFIRM = ClientOpcode.EXCHANGE_CONFIRM;
    public static final int CLIENT_EXCHANGE_ADD_ITEM = ClientOpcode.EXCHANGE_ADD_ITEM;
    public static final int CLIENT_EXCHANGE_APPROVE = ClientOpcode.EXCHANGE_APPROVE;
    public static final int CLIENT_EXCHANGE_CANCEL = ClientOpcode.EXCHANGE_CANCEL;
    public static final int CLIENT_EXCHANGE_FINALIZE = ClientOpcode.EXCHANGE_FINALIZE;

    private TradePackets() {
    }

    public static void sendExchangeRequest(IWorkflowContext ctx, long targetSpawnId) {
        MutablePacket packet = MutablePacket.getBuilder(4, CLIENT_EXCHANGE_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putInt((int) targetSpawnId)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendExchangeConfirm(IWorkflowContext ctx) {
        MutablePacket packet = MutablePacket.getBuilder(0, CLIENT_EXCHANGE_CONFIRM)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendExchangeAddItem(IWorkflowContext ctx, int exchangeId, int srcInventorySlot, int dstOfferSlot, int quantity) {
        MutablePacket packet = MutablePacket.getBuilder(12, CLIENT_EXCHANGE_ADD_ITEM)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putInt(exchangeId)
                .put((byte) (srcInventorySlot & 0xFF))
                .put((byte) (dstOfferSlot & 0xFF))
                .putShort((short) (quantity & 0xFFFF))
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendExchangeApprove(IWorkflowContext ctx) {
        MutablePacket packet = MutablePacket.getBuilder(0, CLIENT_EXCHANGE_APPROVE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendExchangeFinalize(IWorkflowContext ctx) {
        MutablePacket packet = MutablePacket.getBuilder(0, CLIENT_EXCHANGE_FINALIZE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }

    public static void sendExchangeCancel(IWorkflowContext ctx) {
        MutablePacket packet = MutablePacket.getBuilder(0, CLIENT_EXCHANGE_CANCEL)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }
}
