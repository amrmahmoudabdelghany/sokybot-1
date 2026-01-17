package org.sokybot.engine.core.waiting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.test.WorkflowTestBase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WaitingStateManager.
 */
@DisplayName("WaitingStateManager Tests")
class WaitingStateManagerTest extends WorkflowTestBase {
    
    private WaitingStateManager waitingManager;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean timerExpired;
    private AtomicBoolean stopRequested;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        scheduler = Executors.newScheduledThreadPool(2);
        timerExpired = new AtomicBoolean(false);
        stopRequested = new AtomicBoolean(false);
        
        waitingManager = new WaitingStateManager(
            scheduler,
            () -> timerExpired.set(true),
            () -> stopRequested.set(true),
            100); // 100ms delay for testing
    }
    
    @Test
    @DisplayName("Should enter waiting state and start timer")
    void testEnterWaiting() throws InterruptedException {
        WorkflowContextImpl context = createWorkflowContext();
        
        waitingManager.enterWaiting(context);
        
        assertTrue(waitingManager.isWaiting());
        
        // Wait for timer to expire
        Thread.sleep(150);
        
        assertTrue(timerExpired.get());
    }
    
    @Test
    @DisplayName("Should exit waiting state and stop timer")
    void testExitWaiting() {
        WorkflowContextImpl context = createWorkflowContext();
        
        waitingManager.enterWaiting(context);
        assertTrue(waitingManager.isWaiting());
        
        waitingManager.exitWaiting();
        assertFalse(waitingManager.isWaiting());
    }
    
    @Test
    @DisplayName("Should clear state data when entering waiting")
    void testClearStateDataOnEnterWaiting() {
        WorkflowContextImpl context = createWorkflowContext();
        
        // Add some state data
        context.getStateData().put("test-key", "test-value");
        assertEquals(1, context.getStateData().size());
        
        // Enter waiting state
        waitingManager.enterWaiting(context);
        
        // State data should be cleared
        assertTrue(context.getStateData().isEmpty());
    }
    
    @Test
    @DisplayName("Should handle stop request during waiting")
    void testHandleStopRequest() throws InterruptedException {
        WorkflowContextImpl context = createWorkflowContext();
        
        waitingManager.enterWaiting(context);
        assertTrue(waitingManager.isWaiting());
        
        // Request stop
        waitingManager.handleStopRequest();
        
        // Stop callback should be called
        assertTrue(stopRequested.get());
    }
    
    @Test
    @DisplayName("Should prevent multiple simultaneous waiting states")
    void testMultipleWaitingStates() {
        WorkflowContextImpl context = createWorkflowContext();
        
        waitingManager.enterWaiting(context);
        assertTrue(waitingManager.isWaiting());
        
        // Attempt to enter waiting again
        waitingManager.enterWaiting(context);
        
        // Should still be waiting (not enter twice)
        assertTrue(waitingManager.isWaiting());
    }
    
    @Test
    @DisplayName("Should respect custom waiting delay")
    void testCustomWaitingDelay() throws InterruptedException {
        WaitingStateManager customManager = new WaitingStateManager(
            scheduler,
            () -> timerExpired.set(true),
            () -> stopRequested.set(true),
            200); // 200ms delay
        
        WorkflowContextImpl context = createWorkflowContext();
        customManager.enterWaiting(context);
        
        // Timer should not expire before delay
        Thread.sleep(50);
        assertFalse(timerExpired.get());
        
        // Timer should expire after delay
        Thread.sleep(200);
        assertTrue(timerExpired.get());
    }
    
    @Test
    @DisplayName("Should handle rapid enter/exit cycles")
    void testRapidEnterExit() {
        WorkflowContextImpl context = createWorkflowContext();
        
        for (int i = 0; i < 10; i++) {
            waitingManager.enterWaiting(context);
            assertTrue(waitingManager.isWaiting());
            
            waitingManager.exitWaiting();
            assertFalse(waitingManager.isWaiting());
        }
    }
}
