package org.sokybot.engine.api.workflow;

/**
 * Lifecycle hooks for a state within a cycle.
 */
public interface IStateLifecycle {
    
    /**
     * Called when entering this state.
     * Use for state initialization, starting background tasks, etc.
     * 
     * @param context The workflow context
     */
    default void onEnter(IWorkflowContext context) {
        // Optional: Override for state initialization
    }
    
    /**
     * Called when exiting this state.
     * Use for cleanup, canceling background tasks, etc.
     * 
     * @param context The workflow context
     */
    default void onExit(IWorkflowContext context) {
        // Optional: Override for cleanup
    }
}
