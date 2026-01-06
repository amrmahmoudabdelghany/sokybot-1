package org.sokybot.proxy;

/**
 * Factory for creating proxy connections.
 * This service is registered in the OSGi service registry.
 */
public interface IProxyConnectionFactory {
    
    /**
     * Creates a new proxy connection for a machine.
     * Each machine should have its own dedicated proxy connection.
     * 
     * @param machineId unique identifier for the machine
     * @param listener listener for connection lifecycle events
     * @return a new proxy connection instance
     */
    IProxyConnection createConnection(String machineId, IConnectionListener listener);
    
    /**
     * Shuts down all proxy connections and releases resources.
     */
    void shutdown();
}
