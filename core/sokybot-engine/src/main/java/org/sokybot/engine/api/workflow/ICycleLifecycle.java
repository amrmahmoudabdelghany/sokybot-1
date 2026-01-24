package org.sokybot.engine.api.workflow;

/**
 * Lifecycle hooks for a cycle.
 */
public interface ICycleLifecycle {
    
    /**
     * Called when cycle is entered (before first state).
     * 
     * @param context The workflow context
     */
    default void onCycleEnter(IWorkflowContext context) {
        // Optional
    }
    
    /**
     * Called when cycle is exited (after last state or guard failure).
     * 
     * @param context The workflow context
     */
    default void onCycleExit(IWorkflowContext context) {
        // Optional
    }
}
