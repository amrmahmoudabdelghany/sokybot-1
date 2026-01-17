package org.sokybot.engine.core.waiting;

import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages WAITING state lifecycle.
 * Handles 500ms timer, interruption window, and state data clearing.
 */
public class WaitingStateManager {
    
    private static final Logger log = LoggerFactory.getLogger(WaitingStateManager.class);
    private static final long DEFAULT_WAITING_DELAY_MS = 500;
    
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean waitingActive = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> waitingTimer;
    private final Runnable onTimerExpire;
    private final Runnable onStopRequested;
    
    private final long waitingDelayMs;
    
    public WaitingStateManager(ScheduledExecutorService scheduler, 
                               Runnable onTimerExpire,
                               Runnable onStopRequested) {
        this(scheduler, onTimerExpire, onStopRequested, DEFAULT_WAITING_DELAY_MS);
    }
    
    public WaitingStateManager(ScheduledExecutorService scheduler,
                               Runnable onTimerExpire,
                               Runnable onStopRequested,
                               long waitingDelayMs) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Scheduler cannot be null");
        }
        if (onTimerExpire == null) {
            throw new IllegalArgumentException("Timer expire callback cannot be null");
        }
        if (onStopRequested == null) {
            throw new IllegalArgumentException("Stop requested callback cannot be null");
        }
        
        this.scheduler = scheduler;
        this.onTimerExpire = onTimerExpire;
        this.onStopRequested = onStopRequested;
        this.waitingDelayMs = waitingDelayMs;
    }
    
    /**
     * Enters WAITING state.
     * Starts timer and clears state data.
     * 
     * @param context The workflow context
     */
    public void enterWaiting(IWorkflowContext context) {
        if (!waitingActive.compareAndSet(false, true)) {
            log.warn("Attempted to enter waiting state while already waiting");
            return;
        }
        
        log.debug("Entering WAITING state");
        
        // Clear state-local data
        if (context instanceof WorkflowContextImpl) {
            ((WorkflowContextImpl) context).clearStateData();
        }
        
        // Start timer
        startTimer();
    }
    
    /**
     * Exits WAITING state.
     * Stops timer and resets state.
     */
    public void exitWaiting() {
        if (!waitingActive.compareAndSet(true, false)) {
            return; // Already exited
        }
        
        log.debug("Exiting WAITING state");
        stopTimer();
    }
    
    /**
     * Handles stop request during waiting.
     * Can interrupt the waiting state.
     */
    public void handleStopRequest() {
        if (waitingActive.get()) {
            log.info("Stop requested during WAITING state");
            stopTimer();
            onStopRequested.run();
            waitingActive.set(false);
        }
    }
    
    /**
     * Checks if currently in WAITING state.
     * 
     * @return true if in waiting state
     */
    public boolean isWaiting() {
        return waitingActive.get();
    }
    
    private void startTimer() {
        stopTimer(); // Stop any existing timer
        
        waitingTimer = scheduler.schedule(() -> {
            if (waitingActive.get()) {
                log.debug("WAITING timer expired, transitioning to first cycle");
                waitingActive.set(false);
                onTimerExpire.run();
            }
        }, waitingDelayMs, TimeUnit.MILLISECONDS);
    }
    
    private void stopTimer() {
        if (waitingTimer != null && !waitingTimer.isDone()) {
            waitingTimer.cancel(false);
            waitingTimer = null;
        }
    }
    
    /**
     * Shuts down the waiting state manager.
     * Stops any active timers.
     */
    public void shutdown() {
        stopTimer();
        waitingActive.set(false);
    }
}
