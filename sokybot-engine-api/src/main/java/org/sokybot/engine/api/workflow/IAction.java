package org.sokybot.engine.api.workflow;

/**
 * Action to execute when guard passes.
 * Actions perform actual work (e.g., send packets, update state).
 */
public interface IAction {
    
    /**
     * Executes the action.
     * 
     * @param context The workflow context
     * @throws WorkflowException if action fails (engine handles)
     */
    void execute(IWorkflowContext context);
    
    /**
     * Gets a human-readable description of what this action does.
     * Used for logging and debugging.
     * 
     * @return Description string
     */
    default String getDescription() {
        return getClass().getSimpleName();
    }
}
