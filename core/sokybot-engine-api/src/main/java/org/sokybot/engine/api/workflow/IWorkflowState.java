package org.sokybot.engine.api.workflow;

/**
 * Base interface for all workflow states.
 * Provides common state functionality.
 */
public interface IWorkflowState {
    StateId getStateId();
    
    /**
     * Gets the guard that determines if the action should execute.
     * 
     * @return The guard, or null if action should always execute
     */
    IGuard getGuard();
    
    /**
     * Gets the action to execute if guard passes.
     * 
     * @return The action, or null if no action
     */
    IAction getAction();
    
    /**
     * Gets the next state name when guard passes.
     * 
     * @return Next state name, or null to exit cycle
     */
    StateId getNextState();
    
    /**
     * Gets the target state name when guard fails.
     * Can reference any state in the cycle, including earlier states for loops.
     * 
     * @return Target state name (for loops), or null to exit cycle
     */
    StateId getTargetState();
    
    /**
     * Gets custom delay after action execution (ms).
     * If null, uses engine default (500ms).
     * 
     * @return Custom delay in ms, or null for default
     */
    Integer getCustomDelay();
    
    /**
     * Gets lifecycle hooks for this state.
     * 
     * @return State lifecycle, or null
     */
    IStateLifecycle getLifecycle();

    @Deprecated
    default String getName() {
        StateId id = getStateId();
        return id != null ? id.asString() : null;
    }

    @Deprecated
    default String getNextStateName() {
        StateId id = getNextState();
        return id != null ? id.asString() : null;
    }

    @Deprecated
    default String getTargetStateName() {
        StateId id = getTargetState();
        return id != null ? id.asString() : null;
    }
}
