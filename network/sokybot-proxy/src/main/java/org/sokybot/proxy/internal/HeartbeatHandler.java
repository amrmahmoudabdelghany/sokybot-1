package org.sokybot.proxy.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sokybot.network.packet.ClientOpcode;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.ProxyConnection;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

@Sharable
public class HeartbeatHandler extends ChannelDuplexHandler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatHandler.class);
    private static final int HEARTBEAT_OPCODE = ClientOpcode.GATEWAY_PATCH_PING;

    private final ProxyConnection proxyConnection;

    public HeartbeatHandler(ProxyConnection proxyConnection) {
        this.proxyConnection = proxyConnection;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            super.userEventTriggered(ctx, evt);
            return;
        }
        IdleStateEvent e = (IdleStateEvent) evt;
        if (e.state() == IdleState.WRITER_IDLE) {
            if (proxyConnection.shouldInjectHeartbeat()) {
                MutablePacket heartbeat = MutablePacket.getBuilder(0, HEARTBEAT_OPCODE).build();
                ctx.writeAndFlush(heartbeat);
                proxyConnection.onHeartbeatSent();
            }
            return;
        }
        if (e.state() == IdleState.READER_IDLE) {
            log.warn("Proxy [{}] read-idle timeout detected, closing server channel", proxyConnection.getMachineId());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }
}
