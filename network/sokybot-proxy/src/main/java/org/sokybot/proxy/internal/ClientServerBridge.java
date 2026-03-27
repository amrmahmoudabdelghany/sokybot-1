package org.sokybot.proxy.internal;

import java.util.Set;

import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.GlobalOpcode;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.IStreamReader;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.packet.ServerOpcode;
import org.sokybot.proxy.ProxyConnection;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Bridges packets between client and server channels.
 * Filters certain opcodes that should not be forwarded.
 */
@Sharable
public class ClientServerBridge extends SimpleChannelInboundHandler<ImmutablePacket> {

    // Handshake protocol opcodes
    private static final int SETUP_OPCODE = 0x5000;
    private static final int CHALLENGE_OPCODE = 0x5001;
    private static final int ID_OPCODE = 0x2001;

    private final ProxyConnection proxyConnection;

    private final Set<Integer> preventedClientOpcodes;
    private final Set<Integer> preventedServerOpcodes;

    public ClientServerBridge(ProxyConnection proxyConnection) {
        this.proxyConnection = proxyConnection;

        // Opcodes that should not be forwarded from client to server
        this.preventedClientOpcodes = Set.of(
                GlobalOpcode.HANDSHAKE_ACCEPTANCE,
                GlobalOpcode.MODULE_IDENTIFICATION,
                GlobalOpcode.HANDSHAKE,
                ClientOpcode.AUTH_REQUEST,
                ClientOpcode.JOIN_REQUEST);

        // Opcodes that should not be forwarded from server to client
        this.preventedServerOpcodes = Set.of(
                ServerOpcode.LOGIN_RESPONSE,
                ServerOpcode.AUTH_RESPONSE);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket msg) throws Exception {
        NetworkPeer peer = ctx.channel().attr(NetworkAttributes.TRANSPORT).get();
        int opcode = msg.getOpcode();

        // Handle handshake and identification packets from server in clientless mode
        if (peer == NetworkPeer.SERVER && proxyConnection.isClientlessMode()) {
            if (opcode == ServerOpcode.LOGIN_RESPONSE) {
                handleLoginResponse(msg);
            } else if (opcode == ServerOpcode.AUTH_RESPONSE) {
                handleAuthResponse(msg);
            }

            HandshakeHandler handler = null;
            if (opcode == SETUP_OPCODE) {
                handler = proxyConnection.getOrCreateHandshakeHandler();
                handler.handleSetupPacket(msg);
            } else if (opcode == CHALLENGE_OPCODE) {
                handler = proxyConnection.getOrCreateHandshakeHandler();
                handler.handleChallengePacket(msg);
            } else if (opcode == ID_OPCODE) {
                handler = proxyConnection.getOrCreateHandshakeHandler();
                handler.handleServerIdentification(msg);
            }

            if (handler != null) {
                ctx.fireChannelRead(msg);
                return;
            }
        }

        boolean blocked = (peer == NetworkPeer.CLIENT && preventedClientOpcodes.contains(opcode))
                || (peer == NetworkPeer.SERVER && preventedServerOpcodes.contains(opcode));

        MutablePacket packet = MutablePacket.wrap(msg.toBytes());
        packet.setPacketSource(peer);

        if (peer == NetworkPeer.CLIENT) {
            // Forward client packet to server (if not blocked)
            if (!blocked) {
                Channel serverChannel = proxyConnection.getGameServerChannel();
                if (serverChannel != null && serverChannel.isActive()) {
                    serverChannel.writeAndFlush(packet);
                }
            }
        } else {
            // Forward server packet to client (if not blocked)
            if (!blocked) {
                Channel clientChannel = proxyConnection.getClientChannel();
                if (clientChannel != null && clientChannel.isActive()) {
                    clientChannel.writeAndFlush(packet);
                }
            }
        }

        // Continue pipeline for packet observers
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NetworkPeer peer = ctx.channel().attr(NetworkAttributes.TRANSPORT).get();
        if (peer == NetworkPeer.SERVER) {
            System.out.println("Sokybot Proxy: Game server connection closed");
            proxyConnection.onServerDisconnected();
        } else if (peer == NetworkPeer.CLIENT) {
            System.out.println("Sokybot Proxy: Client connection closed");
            proxyConnection.onClientDisconnected();
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("ClientServerBridge error: " + cause.getMessage());
        proxyConnection.onConnectionError(cause);
    }

    private void handleLoginResponse(ImmutablePacket packet) {
        try {
            IStreamReader r = packet.getStreamReader();
            byte resultCode = r.getByte();
            if (resultCode == 0x01) {
                int loginId = r.getInt();
                String agentHost = r.getString();
                int agentPort = r.getShort() & 0xFFFF;
                proxyConnection.scheduleRedirect(agentHost, agentPort, loginId);
            }
        } catch (Exception ignored) {
            // Keep packet flow resilient if parsing fails.
        }
    }

    private void handleAuthResponse(ImmutablePacket packet) {
        try {
            IStreamReader r = packet.getStreamReader();
            byte resultCode = r.getByte();
            boolean success = (resultCode != 0x02);
            if (success) {
                proxyConnection.onAuthSuccess();
            } else {
                proxyConnection.onAuthFailed(resultCode);
            }
        } catch (Exception ignored) {
            // Keep packet flow resilient if parsing fails.
        }
    }
}
