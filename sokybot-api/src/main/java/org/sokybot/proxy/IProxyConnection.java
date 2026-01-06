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
     * Gets the packet publisher for subscribing to incoming packets.
     * @return the packet publisher
     */
    IPacketPublisher getPacketPublisher();
    
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
     * Sets whether the proxy operates in clientless mode (bot without game client).
     * @param clientlessMode true for clientless operation
     */
    void setClientlessMode(boolean clientlessMode);
    
    /**
     * Checks if the proxy is in clientless mode.
     * @return true if clientless mode is enabled
     */
    boolean isClientlessMode();
}
