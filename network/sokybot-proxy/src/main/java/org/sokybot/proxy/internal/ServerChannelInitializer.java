package org.sokybot.proxy.internal;

import org.osgi.service.event.EventAdmin;
import org.sokybot.network.NetworkPeer;
import org.sokybot.proxy.ProxyConnection;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;

/**
 * Initializes the pipeline for game server connections (proxy -> server).
 */
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ProxyConnection proxyConnection;
    private final NetworkComponents networkComponents;
    private final EventAdmin eventAdmin;
    private final ChannelGroup channelGroup;

    public ServerChannelInitializer(ProxyConnection proxyConnection,
            NetworkComponents networkComponents,
            EventAdmin eventAdmin,
            ChannelGroup channelGroup) {
        this.proxyConnection = proxyConnection;
        this.networkComponents = networkComponents;
        this.eventAdmin = eventAdmin;
        this.channelGroup = channelGroup;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.attr(NetworkAttributes.TRANSPORT).set(NetworkPeer.SERVER);

        ch.pipeline()
                .addLast(new PacketDecoder(networkComponents.getBlowfish()))
                .addLast(new PacketEncoder(networkComponents))
                .addLast(new ClientServerBridge(proxyConnection))
                .addLast(new MassiveHandler())
                .addLast((io.netty.channel.ChannelHandler) proxyConnection.getPacketPublisher());

        channelGroup.add(ch);
    }
}
