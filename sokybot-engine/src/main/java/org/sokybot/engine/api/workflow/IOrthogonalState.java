package org.sokybot.engine.api.workflow;

/**
 * Represents an orthogonal cycle state (single state in the overall cycle).
 * These are standalone states that participate in the main cycle flow.
 */
public interface IOrthogonalState extends IWorkflowState {
    
    /**
     * Gets the unique name of this state.
     * Must be unique across all registered states.
     * 
     * @return State name (e.g., "CHECKING_POTION")
     */
    @Override
    String getName();
    
    /**
     * Gets the desired priority for this state.
     * Lower values execute earlier in the cycle.
     * 
     * @return Desired priority (0-9999)
     */
    int getDesiredPriority();
}
