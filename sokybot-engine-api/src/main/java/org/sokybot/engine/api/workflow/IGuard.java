package org.sokybot.engine.api.workflow;

/**
 * Guard that determines if an action should execute.
 * Guards are evaluated before actions in cycle states.
 */
public interface IGuard {
    
    /**
     * Evaluates whether the action should execute.
     * 
     * @param context The workflow context
     * @return true if action should execute, false otherwise
     * @throws WorkflowException if evaluation fails (engine handles)
     */
    boolean evaluate(IWorkflowContext context);
    
    /**
     * Gets a human-readable description of what this guard checks.
     * Used for logging and debugging.
     * 
     * @return Description string
     */
    default String getDescription() {
        return getClass().getSimpleName();
    }
}
