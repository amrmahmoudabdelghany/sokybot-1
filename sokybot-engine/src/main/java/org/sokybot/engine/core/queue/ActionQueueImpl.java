package org.sokybot.engine.core.queue;

import org.sokybot.engine.api.workflow.IAction;
import org.sokybot.engine.api.workflow.IActionQueue;
import org.sokybot.engine.api.workflow.IQueuedAction;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of action queue with priority ordering.
 * Thread-safe priority queue for actions.
 */
public class ActionQueueImpl implements IActionQueue {
    
    private static final int DEFAULT_MAX_SIZE = 1000;
    
    private final PriorityQueue<QueuedActionImpl> queue;
    private final int maxSize;
    private final ReentrantLock lock;
    
    public ActionQueueImpl() {
        this(DEFAULT_MAX_SIZE);
    }
    
    public ActionQueueImpl(int maxSize) {
        this.maxSize = maxSize;
        // Priority queue with custom comparator (higher priority first, FIFO for same priority)
        this.queue = new PriorityQueue<>(Comparator.comparing(QueuedActionImpl::getPriority, Comparator.reverseOrder())
                                                   .thenComparing(QueuedActionImpl::getEnqueueTimestamp));
        this.lock = new ReentrantLock();
    }
    
    @Override
    public void enqueue(IAction action, int priority, String sourceCycle) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null");
        }
        
        lock.lock();
        try {
            if (queue.size() >= maxSize) {
                throw new IllegalStateException("Action queue is full (max size: " + maxSize + ")");
            }
            
            QueuedActionImpl queuedAction = new QueuedActionImpl(action, priority, sourceCycle);
            queue.offer(queuedAction);
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public IQueuedAction dequeue() {
        lock.lock();
        try {
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.lock();
        try {
            queue.clear();
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public int getMaxSize() {
        return maxSize;
    }
}
