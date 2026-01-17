package org.sokybot.engine.core.queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.workflow.IAction;
import org.sokybot.engine.api.workflow.IQueuedAction;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ActionQueueImpl.
 */
@DisplayName("ActionQueueImpl Tests")
class ActionQueueImplTest {
    
    private ActionQueueImpl actionQueue;
    
    @BeforeEach
    void setUp() {
        actionQueue = new ActionQueueImpl();
    }
    
    @Test
    @DisplayName("Should enqueue and dequeue actions in priority order")
    void testEnqueueDequeuePriorityOrder() {
        IAction action1 = createMockAction("action1");
        IAction action2 = createMockAction("action2");
        IAction action3 = createMockAction("action3");
        
        // Enqueue with different priorities (higher priority = executed first)
        actionQueue.enqueue(action1, 100, "cycle1"); // Low priority
        actionQueue.enqueue(action2, 300, "cycle2"); // High priority
        actionQueue.enqueue(action3, 200, "cycle3"); // Medium priority
        
        // Dequeue should return highest priority first
        IQueuedAction dequeued1 = actionQueue.dequeue();
        assertEquals(300, dequeued1.getPriority());
        assertEquals(action2, dequeued1.getAction());
        
        IQueuedAction dequeued2 = actionQueue.dequeue();
        assertEquals(200, dequeued2.getPriority());
        assertEquals(action3, dequeued2.getAction());
        
        IQueuedAction dequeued3 = actionQueue.dequeue();
        assertEquals(100, dequeued3.getPriority());
        assertEquals(action1, dequeued3.getAction());
    }
    
    @Test
    @DisplayName("Should return null when dequeuing from empty queue")
    void testDequeueEmptyQueue() {
        assertTrue(actionQueue.isEmpty());
        assertNull(actionQueue.dequeue());
    }
    
    @Test
    @DisplayName("Should track queue size correctly")
    void testQueueSize() {
        assertEquals(0, actionQueue.size());
        assertTrue(actionQueue.isEmpty());
        
        actionQueue.enqueue(createMockAction("action1"), 100, "cycle1");
        assertEquals(1, actionQueue.size());
        assertFalse(actionQueue.isEmpty());
        
        actionQueue.enqueue(createMockAction("action2"), 200, "cycle2");
        assertEquals(2, actionQueue.size());
        
        actionQueue.dequeue();
        assertEquals(1, actionQueue.size());
        
        actionQueue.dequeue();
        assertEquals(0, actionQueue.size());
        assertTrue(actionQueue.isEmpty());
    }
    
    @Test
    @DisplayName("Should clear all actions from queue")
    void testClear() {
        actionQueue.enqueue(createMockAction("action1"), 100, "cycle1");
        actionQueue.enqueue(createMockAction("action2"), 200, "cycle2");
        
        assertFalse(actionQueue.isEmpty());
        actionQueue.clear();
        
        assertTrue(actionQueue.isEmpty());
        assertEquals(0, actionQueue.size());
        assertNull(actionQueue.dequeue());
    }
    
    @Test
    @DisplayName("Should throw exception when enqueueing null action")
    void testEnqueueNullAction() {
        assertThrows(IllegalArgumentException.class, () -> {
            actionQueue.enqueue(null, 100, "cycle1");
        });
    }
    
    @Test
    @DisplayName("Should respect max size limit")
    void testMaxSizeLimit() {
        ActionQueueImpl limitedQueue = new ActionQueueImpl(2);
        
        limitedQueue.enqueue(createMockAction("action1"), 100, "cycle1");
        limitedQueue.enqueue(createMockAction("action2"), 200, "cycle2");
        
        // Third enqueue should fail
        assertThrows(IllegalStateException.class, () -> {
            limitedQueue.enqueue(createMockAction("action3"), 300, "cycle3");
        });
    }
    
    @Test
    @DisplayName("Should handle same priority FIFO ordering")
    void testSamePriorityFIFO() {
        IAction action1 = createMockAction("action1");
        IAction action2 = createMockAction("action2");
        IAction action3 = createMockAction("action3");
        
        // Enqueue with same priority - should maintain FIFO order
        actionQueue.enqueue(action1, 100, "cycle1");
        actionQueue.enqueue(action2, 100, "cycle2");
        actionQueue.enqueue(action3, 100, "cycle3");
        
        // Should dequeue in FIFO order for same priority
        IQueuedAction dequeued1 = actionQueue.dequeue();
        assertEquals(action1, dequeued1.getAction());
        
        IQueuedAction dequeued2 = actionQueue.dequeue();
        assertEquals(action2, dequeued2.getAction());
        
        IQueuedAction dequeued3 = actionQueue.dequeue();
        assertEquals(action3, dequeued3.getAction());
    }
    
    @Test
    @DisplayName("Should handle thread safety with concurrent access")
    void testThreadSafety() throws InterruptedException {
        final int numThreads = 10;
        final int actionsPerThread = 100;
        List<Thread> threads = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        
        // Enqueue actions from multiple threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Thread t = new Thread(() -> {
                try {
                    for (int j = 0; j < actionsPerThread; j++) {
                        actionQueue.enqueue(
                            createMockAction("action-" + threadId + "-" + j),
                            threadId * 10 + j,
                            "cycle-" + threadId);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                }
            });
            threads.add(t);
            t.start();
        }
        
        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }
        
        // Verify no exceptions occurred
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent enqueue");
        
        // Verify all actions were enqueued
        assertEquals(numThreads * actionsPerThread, actionQueue.size());
        
        // Dequeue all and verify they're in priority order
        IQueuedAction previous = null;
        while (!actionQueue.isEmpty()) {
            IQueuedAction current = actionQueue.dequeue();
            if (previous != null) {
                assertTrue(current.getPriority() <= previous.getPriority(),
                    "Actions should be dequeued in priority order (descending)");
            }
            previous = current;
        }
    }
    
    private IAction createMockAction(String name) {
        return context -> {
            // Mock action does nothing
        };
    }
}
