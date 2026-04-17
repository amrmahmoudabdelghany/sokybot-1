package org.sokybot.engine.api.workflow;

/**
 * Conditional transition definition.
 * Represents one path in a conditional state.
 */
public interface IConditionalTransition {
    
    /**
     * Guard that determines if this transition should be taken.
     * 
     * @return The guard
     */
    IGuard getGuard();
    
    /**
     * State to transition to if guard passes.
     * 
     * @return Target state name
     */
    StateId getTargetState();
    
    /**
     * Optional action for this transition.
     * Executed when this transition is taken.
     * 
     * @return Action, or null
     */
    default IAction getAction() {
        return null;
    }

    @Deprecated
    default String getTargetStateName() {
        StateId id = getTargetState();
        return id != null ? id.asString() : null;
    }
}
