package org.sokybot.engine.api.workflow;

import java.util.List;

/**
 * Represents a complete cycle definition.
 * Bundles define cycles with multiple states and transitions.
 */
public interface ICycleDefinition {
    
    /**
     * Gets the unique name of this cycle.
     * Used for identification and logging.
     * 
     * @return Cycle name (e.g., "training-cycle")
     */
    String getName();
    
    /**
     * Gets the desired priority for this cycle.
     * Lower values execute earlier in the overall cycle.
     * 
     * @return Desired priority (0-9999)
     */
    int getDesiredPriority();
    
    /**
     * Gets all states in this cycle.
     * Order determines execution sequence.
     * 
     * @return List of states in execution order
     */
    List<ICycleState> getStates();
    
    /**
     * Gets the entry state (first state in cycle).
     * This is where the cycle starts when activated.
     * 
     * @return The entry state name
     */
    String getEntryStateName();
    
    /**
     * Gets the entry guard for this cycle.
     * If null, cycle always enters when reached.
     * 
     * @return Entry guard, or null
     */
    default IGuard getEntryGuard() {
        return null;
    }
    
    /**
     * Gets the interruption guard.
     * If this guard passes while a lower-priority cycle is running,
     * the lower-priority cycle is interrupted and this cycle executes.
     * 
     * @return Interruption guard, or null if cycle cannot interrupt
     */
    default IGuard getInterruptionGuard() {
        return null;
    }
    
    /**
     * Gets the priority for interruption.
     * Higher priority cycles can interrupt lower priority ones.
     * If null, uses getDesiredPriority().
     * 
     * @return Interruption priority, or desired priority if not set
     */
    default int getInterruptionPriority() {
        return getDesiredPriority();
    }
    
    /**
     * Action to execute when this cycle interrupts another cycle.
     * 
     * @return Interruption action, or null
     */
    default IAction getInterruptionAction() {
        return null;
    }
    
    /**
     * Determines if this cycle can be interrupted by higher priority cycles.
     * If false, cycle runs to completion even if higher priority cycle needs to run.
     * 
     * @return true if interruptible, false otherwise
     */
    default boolean isInterruptible() {
        return true;
    }
    
    /**
     * Checks if this cycle is enabled.
     * Disabled cycles are skipped in the overall workflow.
     * 
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * Gets a state by name.
     * 
     * @param stateName The state name
     * @return The state, or null if not found
     */
    ICycleState getState(String stateName);
    
    /**
     * Gets cycle lifecycle hooks.
     * 
     * @return Cycle lifecycle, or null for default
     */
    default ICycleLifecycle getLifecycle() {
        return null;
    }
}
