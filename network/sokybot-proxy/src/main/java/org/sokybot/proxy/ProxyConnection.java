package org.sokybot.proxy;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.event.EventAdmin;
import org.sokybot.network.NetworkPeer;
import org.sokybot.network.packet.ImmutablePacket;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.network.IPacketPublisher;
import org.sokybot.proxy.internal.SimplePacketPublisher;
import org.sokybot.proxy.internal.ClientChannelInitializer;
import org.sokybot.proxy.internal.HandshakeHandler;
import org.sokybot.proxy.internal.NetworkComponents;
import org.sokybot.proxy.internal.ServerChannelInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * Implementation of IProxyConnection.
 * Manages bidirectional proxy between game client and game server.
 */
public class ProxyConnection implements IProxyConnection {

    private final String machineId;
    private volatile IConnectionListener listener;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final EventAdmin eventAdmin;
    private final SimplePacketPublisher packetPublisher;

    private final ChannelGroup channelGroup;
    private final NetworkComponents networkComponents;

    private Channel serverChannel; // Listening for client
    private Channel clientChannel; // Connection to game client
    private Channel gameServerChannel; // Connection to game server

    private volatile boolean clientConnected = false;
    private volatile boolean serverConnected = false;

    private boolean clientlessMode = false;
    private HandshakeHandler handshakeHandler;
    private volatile boolean redirecting = false;
    private volatile int pendingLoginId = -1;

    public ProxyConnection(String machineId, IConnectionListener listener,
            EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            EventAdmin eventAdmin) {
        this.machineId = machineId;
        this.listener = listener;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.eventAdmin = eventAdmin;

        this.packetPublisher = new SimplePacketPublisher();

        this.channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.networkComponents = new NetworkComponents();
    }

    @Override
    public void startLocalServer(int port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ClientChannelInitializer(this, networkComponents, eventAdmin, channelGroup))
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        try {
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            System.out.println("Sokybot Proxy [" + machineId + "]: Server started on port " + port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to start local server", e);
        }
    }

    @Override
    public void connectToServer(String host, int port) {
        if (serverConnected) {
            return;
        }

        System.out.println("Sokybot Proxy [" + machineId + "]: Connecting to game server " + host + ":" + port);

        // Reset crypto and handshake state for new connection
        networkComponents.reset();
        resetHandshakeHandler();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ServerChannelInitializer(this, networkComponents, eventAdmin, channelGroup))
                .option(ChannelOption.SO_KEEPALIVE, true);

        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            gameServerChannel = future.channel();
            channelGroup.add(gameServerChannel);
            serverConnected = true;

            System.out.println("Sokybot Proxy [" + machineId + "]: Connected to game server " + host + ":" + port);

            if (listener != null) {
                listener.onServerConnected();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to game server", e);
        } catch (Exception e) {
            System.err
                    .println("Sokybot Proxy [" + machineId + "]: Failed to connect to game server: " + e.getMessage());
            // Do not rethrow; let the bot retry connection smoothly on next cycle tick
            // without blowing up the state machine
        }
    }

    @Override
    public void disconnect() {
        System.out.println("Sokybot Proxy [" + machineId + "]: Disconnecting...");

        channelGroup.close();

        if (serverChannel != null) {
            serverChannel.close();
        }

        clientConnected = false;
        serverConnected = false;

        if (listener != null) {
            listener.onDisconnected(null);
        }

        System.out.println("Sokybot Proxy [" + machineId + "]: Disconnected");
    }

    @Override
    public void sendToServer(MutablePacket packet) {
        if (gameServerChannel != null && gameServerChannel.isActive()) {
            gameServerChannel.writeAndFlush(packet);
            publishOutboundPacket(packet);
        }
    }

    @Override
    public void sendToClient(MutablePacket packet) {
        if (clientChannel != null && clientChannel.isActive()) {
            clientChannel.writeAndFlush(packet);
            publishOutboundPacket(packet);
        }
    }

    public void publishOutboundPacket(MutablePacket packet) {
        try {
            byte[] raw = packet.unwrap();
            if (raw == null || raw.length == 0)
                return;
            byte[] copy = Arrays.copyOf(raw, raw.length);
            ImmutablePacket snapshot = ImmutablePacket.wrap(copy, packet.getDataEncoding(), NetworkPeer.BOT);
            packetPublisher.publish(snapshot);
        } catch (Exception e) {
            // Never let sniffer publishing break actual packet sending; log for diagnosis
            System.err.println(
                    "Sokybot Proxy [" + machineId + "]: Failed to publish BOT packet for sniffer: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return clientConnected && serverConnected;
    }

    @Override
    public boolean isClientConnected() {
        return clientConnected;
    }

    @Override
    public boolean isServerConnected() {
        return serverConnected;
    }

    /**
     * Called by ClientChannelInitializer when client connects.
     */
    public void onClientChannelActive(Channel channel) {
        this.clientChannel = channel;
        this.clientConnected = true;
        channelGroup.add(channel);

        System.out.println("Sokybot Proxy [" + machineId + "]: Client connected");

        if (listener != null) {
            listener.onClientConnected();
        }
    }

    /**
     * Called when a connection error occurs.
     */
    public void onConnectionError(Throwable cause) {
        System.err.println("Sokybot Proxy [" + machineId + "]: Connection error: " + cause.getMessage());

        if (listener != null) {
            listener.onDisconnected(cause);
        }
    }

    /**
     * Gets the client channel for the bridge.
     */
    public Channel getClientChannel() {
        return clientChannel;
    }

    /**
     * Gets the server channel for the bridge.
     */
    public Channel getGameServerChannel() {
        return gameServerChannel;
    }

    public String getMachineId() {
        return machineId;
    }

    /**
     * Sets clientless mode (bot operates without game client).
     */
    public void setClientlessMode(boolean clientlessMode) {
        this.clientlessMode = clientlessMode;
    }

    public boolean isClientlessMode() {
        return clientlessMode;
    }

    /**
     * Gets the connection listener for handshake callbacks.
     */
    public IConnectionListener getListener() {
        return listener;
    }

    /**
     * Gets network components for handshake handler.
     */
    public NetworkComponents getNetworkComponents() {
        return networkComponents;
    }

    /**
     * Returns the existing handshake handler, or creates a new one if none exists.
     * The handler is kept across the multi-step handshake (Initialize → Finalize)
     * so that cryptographic state (secrets, keys, initialized flag) is preserved.
     * It is reset on disconnect/reconnect via {@link #resetHandshakeHandler()}.
     */
    public HandshakeHandler getOrCreateHandshakeHandler() {
        if (handshakeHandler == null) {
            handshakeHandler = new HandshakeHandler(
                    networkComponents,
                    listener,
                    gameServerChannel,
                    this,
                    clientlessMode);
        }
        return handshakeHandler;
    }

    /**
     * Clears the handshake handler so a fresh one is created on next connection.
     */
    public void resetHandshakeHandler() {
        this.handshakeHandler = null;
    }

    public void onServerDisconnected() {
        this.serverConnected = false;
        resetHandshakeHandler();
        if (!redirecting && listener != null) {
            listener.onDisconnected(null);
        }
    }

    public void onClientDisconnected() {
        this.clientConnected = false;
        if (listener != null) {
            listener.onDisconnected(null);
        }
    }

    @Override
    public void setConnectionListener(IConnectionListener listener) {
        this.listener = listener;
        // Update existing handshake handler if it exists
        if (handshakeHandler != null) {
            handshakeHandler.setListener(listener);
        }
    }

    @Override
    public IPacketPublisher getPacketPublisher() {
        return this.packetPublisher;
    }

    public void scheduleRedirect(String host, int port, int loginId) {
        this.pendingLoginId = loginId;
        this.redirecting = true;
        this.serverConnected = false;

        if (listener != null) {
            listener.onRedirectRequired(host, port, loginId);
        }

        CompletableFuture.runAsync(() -> {
            try {
                disconnectGameServer();
                connectToServer(host, port);
            } catch (Exception e) {
                this.redirecting = false;
                if (listener != null) {
                    listener.onDisconnected(e);
                }
            }
        });
    }

    public boolean hasPendingAuth() {
        return pendingLoginId >= 0;
    }

    public int consumePendingLoginId() {
        int id = pendingLoginId;
        pendingLoginId = -1;
        return id;
    }

    public void onAuthSuccess() {
        this.redirecting = false;
        if (listener != null) {
            listener.onAuthenticated();
        }
    }

    public void onAuthFailed(byte resultCode) {
        this.redirecting = false;
        if (listener != null) {
            listener.onDisconnected(new RuntimeException("Agent auth failed: code " + resultCode));
        }
    }

    private void disconnectGameServer() {
        if (gameServerChannel != null) {
            gameServerChannel.close().syncUninterruptibly();
            channelGroup.remove(gameServerChannel);
            gameServerChannel = null;
        }
        networkComponents.reset();
        resetHandshakeHandler();
    }
}
