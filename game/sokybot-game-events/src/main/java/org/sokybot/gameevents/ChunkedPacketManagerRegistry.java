package org.sokybot.gameevents;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for ChunkedPacketManager instances keyed by machine full name.
 * Allows translators to access per-bot chunk managers without passing them as parameters.
 * 
 * <p>Chunk managers are registered when bots are created and unregistered when destroyed.
 * Thread-safe implementation using ConcurrentHashMap.
 * 
 * <p>This registry follows the singleton pattern for easy access from translators
 * without requiring OSGi injection.
 */
public class ChunkedPacketManagerRegistry {
    
    private static final ChunkedPacketManagerRegistry INSTANCE = new ChunkedPacketManagerRegistry();
    
    private final Map<String, ChunkedPacketManager> managers = new ConcurrentHashMap<>();
    
    private ChunkedPacketManagerRegistry() {
        // Singleton - prevent instantiation
    }
    
    /**
     * Get the singleton instance of the registry.
     * 
     * @return The registry instance
     */
    public static ChunkedPacketManagerRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a chunk manager for a specific machine.
     * Called when a bot is created.
     * 
     * @param machineFullName The machine identifier (e.g., "group.bot")
     * @param chunkManager The chunk manager for this bot
     */
    public void register(String machineFullName, ChunkedPacketManager chunkManager) {
        if (machineFullName == null || chunkManager == null) {
            throw new IllegalArgumentException("machineFullName and chunkManager must not be null");
        }
        managers.put(machineFullName, chunkManager);
    }
    
    /**
     * Unregister a chunk manager for a specific machine.
     * Called when a bot is destroyed.
     * 
     * @param machineFullName The machine identifier
     */
    public void unregister(String machineFullName) {
        if (machineFullName != null) {
            managers.remove(machineFullName);
        }
    }
    
    /**
     * Get the chunk manager for a specific machine.
     * Returns null if not registered.
     * 
     * @param machineFullName The machine identifier
     * @return The chunk manager, or null if not found
     */
    public ChunkedPacketManager get(String machineFullName) {
        return managers.get(machineFullName);
    }
    
    /**
     * Check if a chunk manager is registered for a machine.
     * 
     * @param machineFullName The machine identifier
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(String machineFullName) {
        return managers.containsKey(machineFullName);
    }
    
    /**
     * Get the number of registered chunk managers.
     * Useful for debugging/monitoring.
     * 
     * @return The number of registered managers
     */
    public int size() {
        return managers.size();
    }

    /**
     * Removes registry entries whose machine full name is not in {@code validFullNames}. Safe for concurrent use.
     */
    public void pruneEntriesNotIn(Set<String> validFullNames) {
        if (validFullNames == null) {
            return;
        }
        for (String key : managers.keySet()) {
            if (!validFullNames.contains(key)) {
                managers.remove(key);
            }
        }
    }
}
