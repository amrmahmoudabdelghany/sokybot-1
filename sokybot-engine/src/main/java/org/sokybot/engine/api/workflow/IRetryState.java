package org.sokybot.engine.api.workflow;

/**
 * Retry state - retry previous state with limits.
 */
public interface IRetryState extends IWorkflowState {
    
    /**
     * State to retry.
     * 
     * @return Retry state name
     */
    String getRetryState();
    
    /**
     * Maximum retry attempts.
     * 
     * @return Max retries
     */
    int getMaxRetries();
    
    /**
     * Delay between retries (ms).
     * 
     * @return Retry delay, or null for no delay
     */
    default Integer getRetryDelay() {
        return null;
    }
    
    /**
     * Guard that determines if retry should occur.
     * If null, retries until max retries reached.
     * 
     * @return Retry guard, or null
     */
    default IGuard getRetryGuard() {
        return null;
    }
    
    /**
     * State to go to if retries exhausted.
     * 
     * @return Exhausted state name
     */
    String getExhaustedState();
}
