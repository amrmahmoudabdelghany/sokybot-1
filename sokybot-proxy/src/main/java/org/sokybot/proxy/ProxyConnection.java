package org.sokybot.proxy;

import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.MutablePacket;
import org.sokybot.proxy.internal.ClientChannelInitializer;
import org.sokybot.proxy.internal.HandshakeHandler;
import org.sokybot.proxy.internal.NetworkComponents;
import org.sokybot.proxy.internal.ServerChannelInitializer;
import org.sokybot.proxy.internal.SimplePacketPublisher;

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
    private final IConnectionListener listener;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    
    private final ChannelGroup channelGroup;
    private final SimplePacketPublisher packetPublisher;
    private final NetworkComponents networkComponents;
    
    private Channel serverChannel;  // Listening for client
    private Channel clientChannel;  // Connection to game client
    private Channel gameServerChannel;  // Connection to game server
    
    private volatile boolean clientConnected = false;
    private volatile boolean serverConnected = false;
    
    private boolean clientlessMode = false;
    private HandshakeHandler handshakeHandler;
    
    public ProxyConnection(String machineId, IConnectionListener listener, 
                          EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.machineId = machineId;
        this.listener = listener;
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        
        this.channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.packetPublisher = new SimplePacketPublisher();
        this.networkComponents = new NetworkComponents();
    }
    
    @Override
    public void startLocalServer(int port) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ClientChannelInitializer(this, networkComponents, packetPublisher, channelGroup))
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
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ServerChannelInitializer(this, networkComponents, packetPublisher, channelGroup))
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
        }
    }
    
    @Override
    public void sendToClient(MutablePacket packet) {
        if (clientChannel != null && clientChannel.isActive()) {
            clientChannel.writeAndFlush(packet);
        }
    }
    
    @Override
    public IPacketPublisher getPacketPublisher() {
        return packetPublisher;
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
     * Creates and returns the handshake handler for this connection.
     */
    public HandshakeHandler createHandshakeHandler() {
        if (handshakeHandler == null) {
            handshakeHandler = new HandshakeHandler(
                networkComponents, 
                listener, 
                gameServerChannel, 
                clientlessMode
            );
        }
        return handshakeHandler;
    }
}
