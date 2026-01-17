package org.sokybot.engine;

import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.settings.Settings;

/**
 * Public interface for a machine engine instance.
 * 
 * Each machine has its own engine instance that contains:
 * - Workflow engine (cycles, states)
 * - Actuators (cycle definitions)
 * - Action queue processor
 * - Priority-based interruption handler
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
     * Starts the engine (workflow engine, actuators, etc.).
     * Engine starts in IDLE state.
     * 
     * @throws IllegalStateException if engine is already started
     */
    void start();
    
    /**
     * Stops the engine and releases all resources.
     * Stops any active cycle immediately.
     * 
     * @throws IllegalStateException if engine is not running
     */
    void stop();
    
    /**
     * Checks if the engine is currently running.
     * 
     * @return true if engine is running
     */
    boolean isRunning();
    
    /**
     * Gets the current engine state (IDLE, ACTIVE, STOPPED).
     * 
     * @return Current engine state
     */
    EngineState getEngineState();
    
    /**
     * Sends a user command/event to the engine.
     * Valid events: "START_TRAINING", "STOP_TRAINING", "CONNECT", "DISCONNECT"
     * 
     * @param eventName The event name
     * @throws IllegalArgumentException if event name is invalid
     * @throws IllegalStateException if engine is not running
     */
    void sendEvent(String eventName);

    /**
     * Gets the settings associated with this engine.
     * 
     * @return The settings object.
     */
    Settings getSettings();
    
    /**
     * Gets the workflow registry for this engine.
     * Used by actuators to register cycles.
     * 
     * @return The workflow registry
     */
    IWorkflowRegistry getWorkflowRegistry();
}
