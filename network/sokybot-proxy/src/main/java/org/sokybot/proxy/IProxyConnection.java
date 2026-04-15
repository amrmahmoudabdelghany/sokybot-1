package org.sokybot.proxy;

import org.sokybot.network.IPacketPublisher;
import org.sokybot.network.packet.MutablePacket;

/**
 * Represents a proxy connection for a single machine (bot).
 * Manages bidirectional communication between game client and game server.
 */
public interface IProxyConnection {
    
    /**
     * Starts the local server that listens for game client connections.
     * @param port the local port to listen on
     */
    void startLocalServer(int port);
    
    /**
     * Connects to the remote game server.
     * @param host the game server hostname or IP
     * @param port the game server port
     */
    void connectToServer(String host, int port);
    
    /**
     * Disconnects both client and server channels and releases resources.
     */
    void disconnect();
    
    /**
     * Sends a packet to the game server.
     * @param packet the packet to send
     */
    void sendToServer(MutablePacket packet);
    
    /**
     * Sends a packet to the game client.
     * @param packet the packet to send
     */
    void sendToClient(MutablePacket packet);
    
    /**
     * Checks if the proxy is connected to both client and server.
     * @return true if both connections are active
     */
    boolean isConnected();
    
    /**
     * Checks if the client is connected to the local proxy server.
     * @return true if client is connected
     */
    boolean isClientConnected();
    
    /**
     * Checks if the proxy is connected to the game server.
     * @return true if server connection is active
     */
    boolean isServerConnected();

    /**
     * True while the outbound TCP channel to the remote game/gateway peer exists and is active.
     * Can disagree briefly with {@link #isServerConnected()} during redirects or if flags desync.
     * Default falls back to {@link #isServerConnected()} for implementations that do not track the channel.
     */
    default boolean isGameServerChannelActive() {
        return isServerConnected();
    }
    
    /**
     * Optional gateway handshake hints for clientless mode: sent as 0x6100 (locale + module + build)
     * after the gateway identifies itself. Call before {@link #connectToServer(String, int)}.
     */
    default void setGatewayHandshakeHints(byte localeByte, String clientModuleName, int clientVersion) {
        // Default: no-op for bridges that do not implement gateway bot login.
    }

    /**
     * Sets whether the proxy operates in clientless mode (bot without game client).
     * @param clientlessMode true for clientless operation
     */
    void setClientlessMode(boolean clientlessMode);
    
    /**
     * Checks if the proxy is in clientless mode.
     * @return true if clientless mode is enabled
     */
    boolean isClientlessMode();
    
    /**
     * Sets the connection listener for lifecycle events.
     * Can be called after connection creation to replace or set the listener.
     * 
     * @param listener the connection listener (can be null)
     */
    void setConnectionListener(IConnectionListener listener);
    
    /**
     * Gets the packet publisher for subscribing to network packets.
     * @return the packet publisher
     */
    IPacketPublisher getPacketPublisher();
    
    /**
     * Gets the unique machine ID associated with this connection.
     * @return the machine ID
     */
    String getMachineId();

    /**
     * Wall-clock ms when gateway agent list (0xA101) was last seen from the game server on this proxy.
     * Consumers may use this to pace outbound 0x6102. Default 0.
     */
    default long getLastGatewayAgentListFromServerWallClockMs() {
        return 0L;
    }

    /** Clears {@link #getLastGatewayAgentListFromServerWallClockMs()} (e.g. after 0x6102 or new 0x6101). */
    default void clearLastGatewayAgentListFromServerWallClockMs() {
    }
}
