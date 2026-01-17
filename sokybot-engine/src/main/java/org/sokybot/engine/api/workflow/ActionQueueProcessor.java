package org.sokybot.engine.api.workflow;

/**
 * Processor for action queue.
 * Handles queue processing before/after state execution.
 */
public interface ActionQueueProcessor {
    
    /**
     * Processes the action queue.
     * Executes actions in priority order until queue is empty or processing should pause.
     * 
     * @param context The workflow context
     * @return Number of actions processed
     */
    int processQueue(IWorkflowContext context);
    
    /**
     * Processes up to a maximum number of actions from the queue.
     * 
     * @param context The workflow context
     * @param maxActions Maximum number of actions to process
     * @return Number of actions actually processed
     */
    int processQueue(IWorkflowContext context, int maxActions);
    
    /**
     * Checks if queue processing should pause.
     * Used to prevent queue from consuming too much time.
     * 
     * @param context The workflow context
     * @return true if processing should pause
     */
    boolean shouldPauseProcessing(IWorkflowContext context);
}
