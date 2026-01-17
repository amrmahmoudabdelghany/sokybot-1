package org.sokybot.engine.api.workflow;

/**
 * Action queue interface for priority-based action execution.
 * Actions are queued and executed by priority.
 */
public interface IActionQueue {
    
    /**
     * Enqueues an action with priority.
     * 
     * @param action The action to enqueue
     * @param priority The priority (0-9999, higher = more urgent)
     * @param sourceCycle The cycle/state that enqueued this action
     * @throws IllegalStateException if queue is full
     */
    void enqueue(IAction action, int priority, String sourceCycle);
    
    /**
     * Dequeues the highest priority action.
     * 
     * @return The queued action, or null if queue is empty
     */
    IQueuedAction dequeue();
    
    /**
     * Checks if the queue is empty.
     * 
     * @return true if empty
     */
    boolean isEmpty();
    
    /**
     * Gets the current queue size.
     * 
     * @return Number of queued actions
     */
    int size();
    
    /**
     * Clears all queued actions.
     */
    void clear();
    
    /**
     * Gets the maximum queue size.
     * 
     * @return Maximum number of actions that can be queued
     */
    int getMaxSize();
}
