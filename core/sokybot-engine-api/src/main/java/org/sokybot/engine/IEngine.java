package org.sokybot.engine;

import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.engine.api.IDispatcher;

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
 * without depending on engine implementation details or any specific framework.
 * 
 * The new engine architecture is framework-agnostic and uses OSGi for service
 * discovery. Legacy Spring State Machine dependencies have been removed.
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
     * @throws IllegalStateException    if engine is not running
     */
    void sendEvent(EngineEvent event);

    /**
     * Gets the workflow registry for this engine.
     * Used by actuators to register cycles.
     * 
     * @return The workflow registry
     */
    IWorkflowRegistry getWorkflowRegistry();

    /**
     * Gets the dispatcher for this engine.
     * 
     * @return The dispatcher
     */
    IDispatcher getDispatcher();

    /**
     * Gets the group name.
     * 
     * @return The group name
     */
    String getGroupName();

    /**
     * Gets the machine name.
     * 
     * @return The machine name
     */
    String getMachineName();

    /**
     * UI-friendly primary/ordered activity projection.
     */
    java.util.List<String> getActiveActivities();
}
