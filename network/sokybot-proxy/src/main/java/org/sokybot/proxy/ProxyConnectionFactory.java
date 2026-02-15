package org.sokybot.proxy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Factory implementation for creating proxy connections.
 * Manages shared Netty resources across all connections.
 */
@Component(service = IProxyConnectionFactory.class)
public class ProxyConnectionFactory implements IProxyConnectionFactory {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final Map<String, ProxyConnection> connections = new ConcurrentHashMap<>();

    private EventAdmin eventAdmin;

    @Reference
    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    @Activate
    public void activate() {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        System.out.println("Sokybot Proxy: Factory activated, Netty groups initialized");
    }

    @Deactivate
    public void deactivate() {
        shutdown();
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

        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();

        System.out.println("Sokybot Proxy: All connections shut down");
    }

    @Override
    public void destroyConnection(String machineId) {
        ProxyConnection connection = connections.remove(machineId);
        if (connection != null) {
            System.out.println("Sokybot Proxy: Destroying connection for machine: " + machineId);
            try {
                connection.disconnect();
            } catch (Exception e) {
                System.err.println("Error disconnecting machine " + machineId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Removes a connection from the factory.
     * Called when a connection is disconnected.
     * 
     * @deprecated Use {@link #destroyConnection(String)} instead.
     */
    @Deprecated
    void removeConnection(String machineId) {
        connections.remove(machineId);
    }
}
