package org.sokybot.engine.core.queue;

import org.sokybot.engine.api.workflow.ActionQueueProcessor;
import org.sokybot.engine.api.workflow.IActionQueue;
import org.sokybot.engine.api.workflow.IQueuedAction;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.engine.api.workflow.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of action queue processor.
 * Processes queued actions in priority order.
 */
public class ActionQueueProcessorImpl implements ActionQueueProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(ActionQueueProcessorImpl.class);
    private static final int DEFAULT_MAX_ACTIONS_PER_CYCLE = 10;
    
    private final IActionQueue actionQueue;
    private final int maxActionsPerCycle;
    
    public ActionQueueProcessorImpl(IActionQueue actionQueue) {
        this(actionQueue, DEFAULT_MAX_ACTIONS_PER_CYCLE);
    }
    
    public ActionQueueProcessorImpl(IActionQueue actionQueue, int maxActionsPerCycle) {
        this.actionQueue = actionQueue;
        this.maxActionsPerCycle = maxActionsPerCycle;
    }
    
    @Override
    public int processQueue(IWorkflowContext context) {
        return processQueue(context, maxActionsPerCycle);
    }
    
    @Override
    public int processQueue(IWorkflowContext context, int maxActions) {
        int processed = 0;
        
        while (processed < maxActions && !actionQueue.isEmpty() && !shouldPauseProcessing(context)) {
            IQueuedAction queued = actionQueue.dequeue();
            if (queued == null) {
                break;
            }
            
            try {
                // Execute action
                queued.getAction().execute(context);
                processed++;
                
                // Log if in debug mode
                if (log.isDebugEnabled()) {
                    log.debug("Executed queued action from {} with priority {}", 
                             queued.getSourceCycle(), queued.getPriority());
                }
                
            } catch (WorkflowException e) {
                // Handle workflow exception
                log.error("Error executing queued action from {}: {}", 
                         queued.getSourceCycle(), e.getMessage(), e);
                
                // Could retry or discard based on retry count
                if (queued.getRetryCount() < 3) {
                    queued.incrementRetryCount();
                    actionQueue.enqueue(queued.getAction(), queued.getPriority(), queued.getSourceCycle());
                    log.warn("Retrying queued action (attempt {})", queued.getRetryCount());
                } else {
                    log.error("Discarding queued action after {} retries", queued.getRetryCount());
                }
                
            } catch (Exception e) {
                // Handle unexpected exceptions
                log.error("Unexpected error executing queued action from {}: {}", 
                         queued.getSourceCycle(), e.getMessage(), e);
                // Don't retry unexpected exceptions
            }
        }
        
        return processed;
    }
    
    @Override
    public boolean shouldPauseProcessing(IWorkflowContext context) {
        // Pause if queue is too large (backpressure)
        if (actionQueue.size() > actionQueue.getMaxSize() * 0.8) {
            log.warn("Action queue is nearly full, pausing processing");
            return true;
        }
        
        // Could add other pause conditions here
        // e.g., if critical action just executed and needs time
        
        return false;
    }
}
