package org.sokybot.engine.api.workflow;

/**
 * Loop state - handles repetitive execution with protection.
 */
public interface ILoopState extends ICycleState {
    
    /**
     * State to loop back to.
     * Must be a state earlier in the cycle.
     * 
     * @return The state name to loop back to
     */
    StateId getLoopBackState();
    
    /**
     * Guard that determines if loop should continue.
     * If null, loops until maxIterations reached.
     * 
     * @return Loop guard, or null for infinite loop
     */
    default IGuard getLoopGuard() {
        return null;
    }
    
    /**
     * Maximum iterations before forcing exit.
     * If null, no limit (not recommended).
     * 
     * @return Max iterations, or null for unlimited
     */
    default Integer getMaxIterations() {
        return null;
    }
    
    /**
     * Delay between loop iterations (ms).
     * Applied after each loop iteration.
     * 
     * @return Loop delay, or null for default
     */
    default Integer getLoopDelay() {
        return null;
    }
    
    /**
     * Counter key in persistent context.
     * If null, uses default: "{cycleName}.{loopBackState}.iterations"
     * 
     * @return Counter key, or null for default
     */
    default String getCounterKey() {
        return null;
    }
    
    /**
     * Action to execute before exiting loop (if max iterations reached).
     * 
     * @return Loop exit action, or null
     */
    default IAction getLoopExitAction() {
        return null;
    }

    @Deprecated
    default String getLoopBackStateName() {
        StateId id = getLoopBackState();
        return id != null ? id.asString() : null;
    }
}
