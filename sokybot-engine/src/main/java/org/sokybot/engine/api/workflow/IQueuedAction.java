package org.sokybot.engine.api.workflow;

/**
 * Represents a queued action with priority and metadata.
 */
public interface IQueuedAction {
    
    /**
     * Gets the action to execute.
     * 
     * @return The action
     */
    IAction getAction();
    
    /**
     * Gets the priority of this action.
     * Higher priority actions execute first.
     * 
     * @return Priority (0-9999)
     */
    int getPriority();
    
    /**
     * Gets the source cycle/state that enqueued this action.
     * 
     * @return Source identifier
     */
    String getSourceCycle();
    
    /**
     * Gets the timestamp when this action was enqueued.
     * 
     * @return Timestamp in milliseconds
     */
    long getEnqueueTimestamp();
    
    /**
     * Gets the retry count for this action.
     * 
     * @return Number of times this action has been retried
     */
    int getRetryCount();
    
    /**
     * Increments the retry count.
     */
    void incrementRetryCount();
}
