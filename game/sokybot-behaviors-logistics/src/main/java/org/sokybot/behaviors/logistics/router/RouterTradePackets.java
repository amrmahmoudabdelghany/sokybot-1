package org.sokybot.behaviors.logistics.router;

import java.util.List;
import java.util.Objects;

import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.model.IPlayer;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.Encoding;
import org.sokybot.network.packet.MutablePacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Epic #24: Silk Road client trade opcodes (protocol-aligned payload shapes).
 */
public final class RouterTradePackets {

    private static final Logger log = LoggerFactory.getLogger(RouterTradePackets.class);

    public static final int CLIENT_TRADE_REQUEST = 0x7081;
    public static final int CLIENT_TRADE_ADD_ITEM = 0x7082;
    public static final int CLIENT_TRADE_CONFIRM = 0x7083;
    public static final int CLIENT_TRADE_APPROVE = 0x7084;

    private RouterTradePackets() {
    }

    /**
     * Trade request: 32-bit target unique id (OID). Returns {@code false} if the character is not visible locally.
     */
    public static boolean sendTradeRequest(IWorkflowContext ctx, String targetCharName) {
        if (ctx == null) {
            return false;
        }
        String needle = targetCharName != null ? targetCharName.trim() : "";
        if (needle.isEmpty()) {
            log.warn("RouterTradePackets: trade request aborted — blank target name");
            return false;
        }
        IGameModel gm = ctx.getGameModel();
        if (gm == null) {
            log.warn("RouterTradePackets: trade request aborted — no game model");
            return false;
        }
        List<IPlayer> players = gm.snapshotAll(IPlayer.class);
        if (players == null || players.isEmpty()) {
            log.warn("RouterTradePackets: trade request aborted — no spawned players for name [{}]", needle);
            return false;
        }
        Integer targetOid = null;
        for (IPlayer entity : players) {
            if (entity == null) {
                continue;
            }
            String name = entity.getName();
            if (name != null && Objects.equals(name, needle)) {
                targetOid = Integer.valueOf(entity.getUniqueId());
                break;
            }
        }
        if (targetOid == null) {
            log.warn(
                    "RouterTradePackets: trade request aborted — entity not spawned locally for name [{}]",
                    needle);
            return false;
        }
        MutablePacket packet = MutablePacket.getBuilder(4, CLIENT_TRADE_REQUEST)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .putInt(targetOid.intValue())
                .build();
        ctx.getDispatcher().sendToServer(packet);
        return true;
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

    public static void sendTradeApprove(IWorkflowContext ctx) {
        if (ctx == null) {
            return;
        }
        MutablePacket packet = MutablePacket.getBuilder(0, CLIENT_TRADE_APPROVE)
                .packetEncoding(Encoding.ENCRYPTED)
                .dataEncoding(Encoding.PLAIN)
                .packetSource(NetworkPeer.BOT)
                .build();
        ctx.getDispatcher().sendToServer(packet);
    }
}
