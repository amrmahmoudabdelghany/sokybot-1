package org.sokybot.engine.api;

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
    void sendToServer(Object packet);
    
    /**
     * Sends a packet to the game client.
     * 
     * @param packet The packet to send
     * @throws DispatchException if packet cannot be sent
     */
    void sendToClient(Object packet);
    
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
}
