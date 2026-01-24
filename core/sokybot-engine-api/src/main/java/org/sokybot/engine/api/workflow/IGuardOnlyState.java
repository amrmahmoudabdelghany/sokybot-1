package org.sokybot.engine.api.workflow;

/**
 * Guard-only state - check condition without action.
 */
public interface IGuardOnlyState extends ICycleState {
    
    /**
     * Guard to evaluate.
     * 
     * @return The guard
     */
    @Override
    IGuard getGuard();
    
    /**
     * State to go to if guard passes.
     * 
     * @return Success state name
     */
    String getSuccessState();
    
    /**
     * State to go to if guard fails.
     * 
     * @return Failure state name
     */
    String getFailureState();
    
    /**
     * Action to execute (typically null for guard-only states).
     * 
     * @return null (guard-only states don't have actions)
     */
    @Override
    default IAction getAction() {
        return null;
    }
}
