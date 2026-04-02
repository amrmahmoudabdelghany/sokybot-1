package org.sokybot.engine.api;
import org.sokybot.network.packet.MutablePacket;

/**
 * Dispatcher for sending packets to the game server/client.
 * Wraps IProxyConnection to provide a framework-agnostic interface.
 */
public interface IDispatcher {

    /**
     * Sends a packet to the game server.
     * 
     * @param packet The packet to send (byte array or packet object)
     * @throws DispatchException if packet cannot be sent
     */
    void sendToServer(MutablePacket packet);

    /**
     * Sends a packet to the game client.
     * 
     * @param packet The packet to send
     * @throws DispatchException if packet cannot be sent
     */
    void sendToClient(MutablePacket packet);

    /**
     * Checks if the proxy is connected to both client and server.
     * 
     * @return true if both connections are active
     */
    boolean isConnected();

    /**
     * Checks if the client is connected to the local proxy server.
     * 
     * @return true if client is connected
     */
    boolean isClientConnected();

    /**
     * Checks if the proxy is connected to the game server.
     * 
     * @return true if server connection is active
     */
    boolean isServerConnected();

    /**
     * Connects to the game server.
     * 
     * @param host The hostname or IP
     * @param port The port
     * @throws DispatchException if connection fails
     */
    void connect(String host, int port);

    /**
     * Sets whether the proxy operates in clientless mode (bot without game client).
     * 
     * @param clientlessMode true for clientless operation
     */
    void setClientlessMode(boolean clientlessMode);

    /**
     * Checks if the proxy is in clientless mode.
     * 
     * @return true if clientless mode is enabled
     */
    boolean isClientlessMode();

    /**
     * Disconnects from the game server.
     */
    void disconnect();

    /**
     * Sends a gateway login request packet (0x6102) using Java-owned protocol serialization.
     */
    void sendLoginRequest(byte locale, String username, String password, int agentId, String charsetName);

    /**
     * Sends agent request packet (0x6101) if permitted by rate limiter/cache policy.
     *
     * @param allowColdStartBypass true to allow one startup bypass token.
     * @return true when request is sent, false when skipped due to cache/rate policy.
     */
    boolean sendAgentRequest(boolean allowColdStartBypass);

    /**
     * Sends logout packet (0x6104) for graceful shutdown paths.
     */
    void sendLogoutRequest();
}
