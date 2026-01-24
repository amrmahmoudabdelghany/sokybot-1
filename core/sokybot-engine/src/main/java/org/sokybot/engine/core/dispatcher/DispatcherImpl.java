package org.sokybot.engine.core.dispatcher;

import org.sokybot.engine.api.DispatchException;
import org.sokybot.engine.api.IDispatcher;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.network.packet.MutablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of dispatcher that wraps IProxyConnection.
 * Provides framework-agnostic packet sending interface.
 */
public class DispatcherImpl implements IDispatcher {
    
    private static final Logger log = LoggerFactory.getLogger(DispatcherImpl.class);
    
    private final IProxyConnection proxyConnection;
    private final String machineId;
    
    public DispatcherImpl(IProxyConnection proxyConnection, String machineId) {
        if (proxyConnection == null) {
            throw new IllegalArgumentException("Proxy connection cannot be null");
        }
        this.proxyConnection = proxyConnection;
        this.machineId = machineId;
    }
    
    @Override
    public void sendToServer(Object packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }
        
        try {
            if (!proxyConnection.isServerConnected()) {
                throw new DispatchException("Not connected to server for machine: " + machineId);
            }
            
            if (packet instanceof MutablePacket) {
                proxyConnection.sendToServer((MutablePacket) packet);
            } else if (packet instanceof byte[]) {
                // Convert byte array to MutablePacket if needed
                // For now, assume it's a MutablePacket or handle conversion
                log.warn("Byte array packets may need conversion to MutablePacket");
                throw new DispatchException("Byte array packets not directly supported");
            } else {
                throw new DispatchException("Unsupported packet type: " + packet.getClass().getName());
            }
            
        } catch (Exception e) {
            if (e instanceof DispatchException) {
                throw e;
            }
            throw new DispatchException("Failed to send packet to server: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void sendToClient(Object packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }
        
        try {
            if (!proxyConnection.isClientConnected()) {
                throw new DispatchException("Not connected to client for machine: " + machineId);
            }
            
            if (packet instanceof MutablePacket) {
                proxyConnection.sendToClient((MutablePacket) packet);
            } else if (packet instanceof byte[]) {
                log.warn("Byte array packets may need conversion to MutablePacket");
                throw new DispatchException("Byte array packets not directly supported");
            } else {
                throw new DispatchException("Unsupported packet type: " + packet.getClass().getName());
            }
            
        } catch (Exception e) {
            if (e instanceof DispatchException) {
                throw e;
            }
            throw new DispatchException("Failed to send packet to client: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isConnected() {
        return proxyConnection.isConnected();
    }
    
    @Override
    public boolean isClientConnected() {
        return proxyConnection.isClientConnected();
    }
    
    @Override
    public boolean isServerConnected() {
        return proxyConnection.isServerConnected();
    }
}
