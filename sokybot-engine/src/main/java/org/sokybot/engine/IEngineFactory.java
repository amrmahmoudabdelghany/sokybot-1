package org.sokybot.engine;

import org.sokybot.proxy.IProxyConnection;

/**
 * Factory for creating engine instances for machines.
 * 
 * Similar to IProxyConnectionFactory, this factory allows other bundles
 * (like context-factory) to create engine instances without depending on
 * engine implementation details.
 * 
 * Each machine gets its own engine instance via this factory.
 */
public interface IEngineFactory {
    
    /**
     * Creates an engine instance for a machine.
     * 
     * @param machineId The full machine ID (format: "groupName.machineName")
     * @param proxyConnection The proxy connection for this machine (provides packets)
     * @param groupName The name of the group this machine belongs to
     * @param machineName The name of the machine
     * @return The created engine instance
     * @throws IllegalStateException if engine already exists for this machine
     */
    IEngine createEngine(String machineId, IProxyConnection proxyConnection, 
                        String groupName, String machineName);
    
    /**
     * Destroys an engine instance and releases resources.
     * 
     * @param machineId The machine ID
     */
    void destroyEngine(String machineId);
    
    /**
     * Gets an existing engine instance.
     * 
     * @param machineId The machine ID
     * @return The engine instance, or null if not found
     */
    IEngine getEngine(String machineId);
}
