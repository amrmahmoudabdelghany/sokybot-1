package org.sokybot.engine;

/**
 * Public interface for a machine engine instance.
 * 
 * Each machine has its own engine instance that contains:
 * - State machine (workflows) - accessed via IMachineContext if needed
 * - Actuators (actions)
 * - Controllers (event handlers)
 * 
 * This interface allows other bundles to interact with engines
 * without depending on engine implementation details or Spring framework.
 * 
 * Note: IPacketPublisher is accessed via IMachineContext.packetPublisher()
 * Note: State machine access (if needed) should be through IMachineContext
 *       to avoid exposing Spring framework types in the API.
 */
public interface IEngine {
    
    /**
     * Gets the machine ID this engine belongs to.
     * Format: "groupName.machineName"
     * 
     * @return The full machine ID
     */
    String getMachineId();
    
    /**
     * Starts the engine (state machine, actuators, etc.).
     */
    void start();
    
    /**
     * Stops the engine and releases resources.
     */
    void stop();
    
    /**
     * Checks if the engine is currently running.
     * 
     * @return true if engine is running
     */
    boolean isRunning();
}
