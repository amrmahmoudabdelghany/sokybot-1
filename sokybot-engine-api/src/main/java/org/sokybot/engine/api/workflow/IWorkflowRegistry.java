package org.sokybot.engine.api.workflow;

import java.util.List;

/**
 * Registry for orthogonal cycle states.
 * Manages cycle registration and ordering.
 */
public interface IWorkflowRegistry {
    
    /**
     * Registers an orthogonal cycle state with desired priority.
     * 
     * Priority determines order (lower = earlier in cycle).
     * If priority conflicts with existing state, actual priority may differ.
     * 
     * @param state The orthogonal state to register
     * @return The actual priority assigned (may differ if conflict)
     * @throws IllegalStateException if engine cycle is active
     * @throws IllegalArgumentException if state name is invalid or duplicate
     */
    int registerOrthogonalState(IOrthogonalState state);
    
    /**
     * Registers an orthogonal state relative to another state.
     * 
     * @param state The orthogonal state to register
     * @param afterState Name of state to place this after (must exist)
     * @param fallbackPriority Priority to use if afterState not found
     * @return The actual priority assigned
     * @throws IllegalStateException if engine cycle is active
     * @throws IllegalArgumentException if state name is invalid or duplicate
     */
    int registerAfter(IOrthogonalState state, String afterState, int fallbackPriority);
    
    /**
     * Unregisters an orthogonal state.
     * 
     * @param stateName The name of the state to remove
     * @return true if state was removed, false if not found
     * @throws IllegalStateException if engine cycle is active
     */
    boolean unregisterOrthogonalState(String stateName);
    
    /**
     * Registers a complete cycle definition.
     * The cycle will be integrated into the overall workflow.
     * 
     * @param cycle The cycle definition to register
     * @return The actual priority assigned
     * @throws IllegalStateException if engine cycle is active
     * @throws IllegalArgumentException if cycle definition is invalid
     */
    int registerCycle(ICycleDefinition cycle);
    
    /**
     * Unregisters a cycle and all its states.
     * 
     * @param cycleName The name of the cycle to remove
     * @return true if cycle was removed, false if not found
     * @throws IllegalStateException if engine cycle is active
     */
    boolean unregisterCycle(String cycleName);
    
    /**
     * Enables or disables a cycle.
     * Disabled cycles are skipped in the workflow.
     * 
     * @param cycleName The name of the cycle
     * @param enabled true to enable, false to disable
     * @throws IllegalArgumentException if cycle not found
     */
    void setCycleEnabled(String cycleName, boolean enabled);
    
    /**
     * Checks if a cycle is enabled.
     * 
     * @param cycleName The name of the cycle
     * @return true if enabled
     */
    boolean isCycleEnabled(String cycleName);
    
    /**
     * Gets all registered cycles.
     * 
     * @return List of cycle names
     */
    List<String> getRegisteredCycles();
    
    /**
     * Gets a registered cycle by name.
     * 
     * @param cycleName The name of the cycle
     * @return The cycle, or null if not found
     */
    ICycleDefinition getCycle(String cycleName);
    
    /**
     * Gets the ordered list of all workflow components.
     * Includes both single states and cycle entry points.
     * 
     * @return List of component names in execution order
     */
    List<String> getOrderedWorkflowComponents();
    
    /**
     * Gets all states (both single states and cycle states).
     * 
     * @return List of all state names (fully qualified: "cycle.state" or just "state")
     */
    List<String> getAllStateNames();
    
    /**
     * Gets a state by its fully qualified name.
     * Fully qualified names: "{cycleName}.{stateName}" for cycle states, 
     * or just "{stateName}" for single states.
     * 
     * @param fullyQualifiedStateName The fully qualified state name
     * @return The state implementation, or null if not found
     */
    IWorkflowState getState(String fullyQualifiedStateName);
    
    /**
     * Checks if engine cycle is currently active.
     * States cannot be registered/unregistered while cycle is active.
     * 
     * @return true if cycle is active
     */
    boolean isCycleActive();
}
