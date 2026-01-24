package org.sokybot.engine.api.workflow;

/**
 * Delay state - explicit delay/throttling point.
 */
public interface IDelayState extends ICycleState {
    
    /**
     * Delay duration in milliseconds.
     * 
     * @return Delay in ms
     */
    int getDelayMs();
    
    /**
     * Minimum delay regardless of other conditions.
     * Prevents rapid transitions.
     * 
     * @return Minimum delay in ms
     */
    default int getMinDelayMs() {
        return 0;
    }
    
    /**
     * Action to execute during delay (optional, on background thread).
     * 
     * @return Delay action, or null
     */
    default IAction getDelayAction() {
        return null;
    }
}
