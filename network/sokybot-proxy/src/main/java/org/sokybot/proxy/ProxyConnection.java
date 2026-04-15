package org.sokybot.proxy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.event.Event;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of IProxyConnection.
 * Manages bidirectional proxy between game client and game server.
 */
public class ProxyConnection implements IProxyConnection {
    private static final Logger log = LoggerFactory.getLogger(ProxyConnection.class);

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
    private volatile long lastPingSentTs = 0L;
    private volatile long latencyMs = -1L;

    /** Set in {@link SimplePacketPublisher} when 0xA101 is read from the game-server channel (clientless-safe). */
    private final AtomicLong lastGatewayAgentListFromServerWallClockMs = new AtomicLong(0L);

    /**
     * When false, {@link HeartbeatHandler} must not inject 0x2002 — avoids pings before the server's
     * first encrypted 0x2001 (MODULE_ID) during handshake.
     */
    private volatile boolean clientlessServerModuleIdentified = false;

    /** Used for clientless 0x6100 after gateway 0x2001 (defaults match common vSRO-style captures). */
    private volatile byte gatewayHandshakeLocale = 22;
    private volatile String gatewayClientModuleName = "SR_Client";
    private volatile int gatewayClientVersion = 188;

    /** Min interval between 0x6102 on the same game-server TCP channel; enforced in {@link org.sokybot.proxy.internal.PacketEncoder#encode}. */
    /** Called from {@link org.sokybot.proxy.internal.SimplePacketPublisher} when 0xA101 is read from the server. */
    public void markGatewayAgentListFromServer(long wallClockMs) {
        lastGatewayAgentListFromServerWallClockMs.set(wallClockMs);
    }

    @Override
    public long getLastGatewayAgentListFromServerWallClockMs() {
        return lastGatewayAgentListFromServerWallClockMs.get();
    }

    @Override
    public void clearLastGatewayAgentListFromServerWallClockMs() {
        lastGatewayAgentListFromServerWallClockMs.set(0L);
    }

    public static long gatewayLoginMinIntervalMs() {
        String p = System.getProperty("sokybot.gateway.loginRequest.minIntervalMs");
        if (p == null || p.isBlank()) {
            return 2500L;
        }
        try {
            return Math.max(0L, Long.parseLong(p.trim()));
        } catch (NumberFormatException e) {
            return 2500L;
        }
    }

    public ProxyConnection(String machineId, IConnectionListener listener,
            EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            EventAdmin eventAdmin) {
        this.machineId = machineId;
        this.listener = listener;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.eventAdmin = eventAdmin;

        this.packetPublisher = new SimplePacketPublisher(this);

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
            log.info("Proxy [{}] skipping connectToServer to {}:{} because serverConnected=true", machineId, host, port);
            return;
        }
        emitNetworkLifecycleEvent("Connecting", false, false, "CONNECTING", null, host, port);

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
            emitNetworkLifecycleEvent("Connected", true, false, "CONNECTED", null, host, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Proxy [{}] interrupted while connecting to {}:{}", machineId, host, port, e);
            emitNetworkLifecycleEvent("Disconnected", false, false, "DISCONNECTED", e.getMessage(), host, port);
            throw new RuntimeException("Failed to connect to game server", e);
        } catch (Exception e) {
            System.err
                    .println("Sokybot Proxy [" + machineId + "]: Failed to connect to game server: " + e.getMessage());
            log.error("Proxy [{}] failed to connect to game server {}:{} (serverConnected={})",
                    machineId, host, port, serverConnected, e);
            emitNetworkLifecycleEvent("Disconnected", false, false, "DISCONNECTED", e.getMessage(), host, port);
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
        lastGatewayAgentListFromServerWallClockMs.set(0L);

        if (listener != null) {
            listener.onDisconnected(null);
        }
        emitNetworkLifecycleEvent("Disconnected", false, false, "DISCONNECTED", null, null, null);

        System.out.println("Sokybot Proxy [" + machineId + "]: Disconnected");
    }

    @Override
    public void sendToServer(MutablePacket packet) {
        // 0x6102 throttling is enforced in PacketEncoder.encode (same classloader as MutablePacket; OSGi-safe).
        if (gameServerChannel != null && gameServerChannel.isActive()) {
            // Publish only after PacketEncoder runs on the event loop (count/CRC + Blowfish).
            gameServerChannel.writeAndFlush(packet).addListener((ChannelFuture future) -> {
                if (future.isSuccess()) {
                    publishOutboundPacket(packet);
                }
            });
        }
    }

    @Override
    public void sendToClient(MutablePacket packet) {
        if (clientChannel != null && clientChannel.isActive()) {
            clientChannel.writeAndFlush(packet).addListener((ChannelFuture future) -> {
                if (future.isSuccess()) {
                    publishOutboundPacket(packet);
                }
            });
        }
    }

    /**
     * Handshake / gateway sends that bypass {@link #sendToServer(MutablePacket)} must still record only
     * after {@link org.sokybot.proxy.internal.PacketEncoder} runs (count, CRC, Blowfish).
     */
    public void writeGameServerAndPublish(MutablePacket packet) {
        if (packet == null || gameServerChannel == null || !gameServerChannel.isActive()) {
            return;
        }
        gameServerChannel.writeAndFlush(packet).addListener((ChannelFuture future) -> {
            if (future.isSuccess()) {
                publishOutboundPacket(packet);
            }
        });
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

    @Override
    public boolean isGameServerChannelActive() {
        Channel ch = gameServerChannel;
        return ch != null && ch.isActive();
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
        emitNetworkLifecycleEvent(
                "Disconnected",
                false,
                false,
                "DISCONNECTED",
                cause != null ? cause.getMessage() : null,
                null,
                null);
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

    @Override
    public void setGatewayHandshakeHints(byte localeByte, String clientModuleName, int clientVersion) {
        String mod = clientModuleName == null || clientModuleName.isBlank() ? "SR_Client" : clientModuleName.trim();
        if (mod.length() > 64) {
            mod = mod.substring(0, 64);
        }
        this.gatewayHandshakeLocale = localeByte;
        this.gatewayClientModuleName = mod;
        this.gatewayClientVersion = clientVersion;
    }

    public byte getGatewayHandshakeLocale() {
        return gatewayHandshakeLocale;
    }

    public String getGatewayClientModuleName() {
        return gatewayClientModuleName;
    }

    public int getGatewayClientVersion() {
        return gatewayClientVersion;
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
     * {@link #resetHandshakeHandler()} clears only handler state; 0x6102 throttle is per game-server {@link Channel}.
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
        this.clientlessServerModuleIdentified = false;
    }

    /** Clientless-only: set after we process the server's first encrypted 0x2001 (service name). */
    public void markClientlessServerModuleIdentified() {
        this.clientlessServerModuleIdentified = true;
    }

    public boolean isClientlessServerModuleIdentified() {
        return clientlessServerModuleIdentified;
    }

    public void onServerDisconnected() {
        this.serverConnected = false;
        this.lastPingSentTs = 0L;
        this.latencyMs = -1L;
        resetHandshakeHandler();
        if (!redirecting && listener != null) {
            listener.onDisconnected(null);
        }
        emitNetworkLifecycleEvent("Disconnected", false, false, "DISCONNECTED", null, null, null);
    }

    public void onClientDisconnected() {
        this.clientConnected = false;
        if (listener != null) {
            listener.onDisconnected(null);
        }
        emitNetworkLifecycleEvent("Disconnected", false, false, "DISCONNECTED", null, null, null);
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
        emitNetworkLifecycleEvent("Authenticated", true, true, "AUTHENTICATED", null, null, null);
    }

    public void onAuthFailed(byte resultCode) {
        this.redirecting = false;
        if (listener != null) {
            listener.onDisconnected(new RuntimeException("Agent auth failed: code " + resultCode));
        }
        emitNetworkLifecycleEvent(
                "Disconnected",
                false,
                false,
                "AUTH_FAILED",
                "Agent auth failed: code " + resultCode,
                null,
                null);
    }

    private void disconnectGameServer() {
        if (gameServerChannel != null) {
            gameServerChannel.close().syncUninterruptibly();
            channelGroup.remove(gameServerChannel);
            gameServerChannel = null;
        }
        networkComponents.reset();
        resetHandshakeHandler();
        this.lastPingSentTs = 0L;
        this.latencyMs = -1L;
    }

    public boolean shouldInjectHeartbeat() {
        // Avoid duplicate 0x2002 flood when physical client is attached; never before server 0x2001.
        return clientlessMode && clientlessServerModuleIdentified;
    }

    public void onHeartbeatSent() {
        lastPingSentTs = System.currentTimeMillis();
    }

    public void onHeartbeatObserved() {
        long sent = lastPingSentTs;
        if (sent <= 0L) {
            return;
        }
        long now = System.currentTimeMillis();
        long delta = now - sent;
        // Filter implausible values to keep UI latency stable and avoid server-initiated ping noise.
        if (delta < 2L || delta > 60000L) {
            return;
        }
        latencyMs = delta;
        emitHeartbeatEvent(delta);
    }

    private void emitHeartbeatEvent(long latency) {
        if (eventAdmin == null) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        props.put("machineId", machineId);
        props.put("transition", "Heartbeat");
        props.put("connected", serverConnected);
        props.put("authenticated", !hasPendingAuth());
        props.put("timestamp", System.currentTimeMillis());
        props.put("latencyMs", latency);
        eventAdmin.postEvent(new Event(
                org.sokybot.commons.osgi.OsgiEventTopics.networkTopic(machineId, "Heartbeat"),
                props));
    }

    private void emitNetworkLifecycleEvent(
            String transition,
            boolean connected,
            boolean authenticated,
            String loginPhase,
            String reason,
            String host,
            Integer port) {
        if (eventAdmin == null) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        props.put("machineId", machineId);
        props.put("transition", transition);
        props.put("connected", connected);
        props.put("authenticated", authenticated);
        if (loginPhase != null && !loginPhase.isEmpty()) {
            props.put("loginPhase", loginPhase);
        }
        props.put("timestamp", System.currentTimeMillis());
        if (latencyMs >= 0L) {
            props.put("latencyMs", latencyMs);
        }
        if (reason != null && !reason.isEmpty()) {
            props.put("reason", reason);
        }
        if (host != null && !host.isEmpty()) {
            props.put("host", host);
        }
        if (port != null) {
            props.put("port", port);
        }

        eventAdmin.postEvent(new Event(
                org.sokybot.commons.osgi.OsgiEventTopics.networkTopic(machineId, transition),
                props));
    }
}
