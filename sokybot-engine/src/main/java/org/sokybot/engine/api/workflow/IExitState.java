package org.sokybot.engine.api.workflow;

/**
 * Exit state - explicit cycle exit point.
 */
public interface IExitState extends IWorkflowState {
    
    /**
     * Guard that determines if cycle should exit.
     * If null, always exits.
     * 
     * @return Exit guard, or null
     */
    default IGuard getExitGuard() {
        return null;
    }
    
    /**
     * Action to execute before exiting cycle.
     * 
     * @return Exit action, or null
     */
    default IAction getExitAction() {
        return null;
    }
    
    /**
     * State to transition to if exit guard fails.
     * If null, continues to next state in cycle.
     * 
     * @return Continue state, or null
     */
    default String getContinueState() {
        return null;
    }
}
