package org.sokybot.proxy;

/**
 * Listener for proxy connection lifecycle events.
 */
public interface IConnectionListener {
    
    /**
     * Called when the game client connects to the local proxy server.
     */
    void onClientConnected();
    
    /**
     * Called when the proxy successfully connects to the game server.
     */
    void onServerConnected();
    
    /**
     * Called after security protocols are configured (Blowfish, CRC, Count).
     */
    void onSecuritySetupComplete();
    
    /**
     * Called when the handshake challenge is successfully completed.
     */
    void onHandshakeComplete();
    
    /**
     * Called when the handshake fails.
     * @param reason the reason for failure
     */
    void onHandshakeFailed(String reason);
    
    /**
     * Called when the proxy detects a redirect request (0xA102).
     */
    void onRedirectRequired(String host, int port, int loginId);
    
    /**
     * Called when the server identifies itself (e.g. GatewayServer, AgentServer).
     * Corresponds to opcode 0x2001.
     */
    void onServerIdentified(String serviceName);
    
    /**
     * Called when either connection is closed.
     * @param cause the reason for disconnection, may be null for normal shutdown
     */
    void onDisconnected(Throwable cause);
}
