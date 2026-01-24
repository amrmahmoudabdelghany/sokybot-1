package org.sokybot.proxy.internal;

import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.proxy.ProxyConnection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handles client channel events (end of pipeline for client channel).
 */
public class ClientHandler extends SimpleChannelInboundHandler<ImmutablePacket> {
    
    private final ProxyConnection proxyConnection;
    
    public ClientHandler(ProxyConnection proxyConnection) {
        this.proxyConnection = proxyConnection;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImmutablePacket packet) throws Exception {
        // Packets are already processed by upstream handlers
        // This handler is at the end of the pipeline for any final processing
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Sokybot Proxy [" + proxyConnection.getMachineId() + "]: Client disconnected");
        super.channelInactive(ctx);
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("Sokybot Proxy: Client connection error: " + cause.getMessage());
        proxyConnection.onConnectionError(cause);
    }
}
