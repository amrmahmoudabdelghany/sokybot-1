package org.sokybot.engine.api.workflow;

/**
 * Represents a state within a cycle definition.
 * States define the behavior at each step of the cycle.
 */
public interface ICycleState extends IWorkflowState {
    
    /**
     * Gets the state type.
     * Determines which specialized interface this state implements.
     * 
     * @return The state type
     */
    StateType getStateType();
    
    /**
     * Gets the state as a specific type.
     * Use to access specialized state functionality.
     * 
     * @param <T> The state interface type
     * @param type The class of the state interface
     * @return The state as the requested type, or null if incompatible
     */
    <T extends IWorkflowState> T getAs(Class<T> type);
}
