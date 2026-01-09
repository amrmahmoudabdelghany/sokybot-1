package org.sokybot.proxy;

/**
 * Factory for creating proxy connections.
 * This service is registered in the OSGi service registry.
 */
public interface IProxyConnectionFactory {
    
    /**
     * Creates a new proxy connection for a machine without a connection listener.
     * The listener can be set later using {@link IProxyConnection#setConnectionListener(IConnectionListener)}.
     * 
     * @param machineId unique identifier for the machine
     * @return a new proxy connection instance
     */
    default IProxyConnection createConnection(String machineId) {
        return createConnection(machineId, null);
    }
    
    /**
     * Creates a new proxy connection for a machine.
     * Each machine should have its own dedicated proxy connection.
     * 
     * @param machineId unique identifier for the machine
     * @param listener listener for connection lifecycle events (can be null, set later)
     * @return a new proxy connection instance
     */
    IProxyConnection createConnection(String machineId, IConnectionListener listener);
    
    /**
     * Shuts down all proxy connections and releases resources.
     */
    void shutdown();
}
