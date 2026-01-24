package org.sokybot.proxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.event.EventAdmin;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Factory implementation for creating proxy connections.
 * Manages shared Netty resources across all connections.
 */
public class ProxyConnectionFactory implements IProxyConnectionFactory {
    
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final Map<String, ProxyConnection> connections;
    private final EventAdmin eventAdmin;
    
    public ProxyConnectionFactory(EventAdmin eventAdmin) {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.connections = new ConcurrentHashMap<>();
        this.eventAdmin = eventAdmin;
    }
    
    @Override
    public IProxyConnection createConnection(String machineId, IConnectionListener listener) {
        if (connections.containsKey(machineId)) {
            throw new IllegalStateException("Connection already exists for machine: " + machineId);
        }
        
        ProxyConnection connection = new ProxyConnection(machineId, listener, bossGroup, workerGroup, eventAdmin);
        connections.put(machineId, connection);
        
        System.out.println("Sokybot Proxy: Created connection for machine: " + machineId);
        return connection;
    }
    
    @Override
    public void shutdown() {
        System.out.println("Sokybot Proxy: Shutting down all connections...");
        
        for (ProxyConnection connection : connections.values()) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                System.err.println("Error disconnecting: " + e.getMessage());
            }
        }
        connections.clear();
        
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        
        System.out.println("Sokybot Proxy: All connections shut down");
    }
    
    /**
     * Removes a connection from the factory.
     * Called when a connection is disconnected.
     */
    void removeConnection(String machineId) {
        connections.remove(machineId);
    }
}

