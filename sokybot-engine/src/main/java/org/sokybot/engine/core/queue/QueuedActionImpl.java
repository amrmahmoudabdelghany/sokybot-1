package org.sokybot.engine.core.queue;

import org.sokybot.engine.api.workflow.IAction;
import org.sokybot.engine.api.workflow.IQueuedAction;

/**
 * Implementation of queued action with priority and metadata.
 */
public class QueuedActionImpl implements IQueuedAction, Comparable<QueuedActionImpl> {
    
    private final IAction action;
    private final int priority;
    private final String sourceCycle;
    private final long enqueueTimestamp;
    private int retryCount;
    
    public QueuedActionImpl(IAction action, int priority, String sourceCycle) {
        this.action = action;
        this.priority = priority;
        this.sourceCycle = sourceCycle;
        this.enqueueTimestamp = System.currentTimeMillis();
        this.retryCount = 0;
    }
    
    @Override
    public IAction getAction() {
        return action;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public String getSourceCycle() {
        return sourceCycle;
    }
    
    @Override
    public long getEnqueueTimestamp() {
        return enqueueTimestamp;
    }
    
    @Override
    public int getRetryCount() {
        return retryCount;
    }
    
    @Override
    public void incrementRetryCount() {
        retryCount++;
    }
    
    /**
     * Compares by priority (higher priority first).
     * For equal priority, compares by timestamp (FIFO).
     */
    @Override
    public int compareTo(QueuedActionImpl other) {
        // Higher priority first
        int priorityCompare = Integer.compare(other.priority, this.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // FIFO for same priority
        return Long.compare(this.enqueueTimestamp, other.enqueueTimestamp);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof QueuedActionImpl)) return false;
        QueuedActionImpl other = (QueuedActionImpl) obj;
        return this.priority == other.priority && 
               this.enqueueTimestamp == other.enqueueTimestamp;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(enqueueTimestamp);
    }
}
