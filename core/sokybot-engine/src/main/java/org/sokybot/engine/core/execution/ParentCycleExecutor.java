package org.sokybot.engine.core.execution;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.interruption.InterruptionManager;
import org.sokybot.engine.core.queue.ActionQueueProcessorImpl;
import org.sokybot.engine.core.waiting.WaitingStateManager;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Executes the parent cycle (orchestrates all orthogonal cycles).
 * Manages WAITING state and cycle transitions.
 */
public class ParentCycleExecutor {

    private static final Logger log = LoggerFactory.getLogger(ParentCycleExecutor.class);

    private final IWorkflowRegistry registry;
    private final ActionQueueProcessorImpl queueProcessor;
    private WaitingStateManager waitingManager;
    private final IWorkflowContext context;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread executorThread;
    private final ReentrantLock waitLock = new ReentrantLock();
    private final Condition waitCondition = waitLock.newCondition();
    private final CycleExecutor cycleExecutor;

    public ParentCycleExecutor(IWorkflowRegistry registry,
            InterruptionManager interruptionManager,
            ActionQueueProcessorImpl queueProcessor,
            WaitingStateManager waitingManager,
            IWorkflowContext context) {
        this.registry = registry;
        this.queueProcessor = queueProcessor;
        this.waitingManager = waitingManager;
        this.context = context;
        this.cycleExecutor = new CycleExecutor(interruptionManager, queueProcessor, context);
    }

    /**
     * Sets the waiting manager.
     * Used for circular dependency resolution.
     */
    public void setWaitingManager(WaitingStateManager waitingManager) {
        this.waitingManager = waitingManager;
    }

    /**
     * Starts the parent cycle execution.
     * Begins with WAITING state.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Parent cycle executor already running");
            return;
        }

        log.info("Starting parent cycle executor");

        log.info("Starting parent cycle executor");

        // Start executor thread
        executorThread = new Thread(this::executeParentCycle, "ParentCycleExecutor-" + context.getMachineId());
        executorThread.setDaemon(true);
        executorThread.start();
    }

    /**
     * Stops the parent cycle execution.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            log.warn("Parent cycle executor not running");
            return;
        }

        log.info("Stopping parent cycle executor");

        // Stop waiting manager
        waitingManager.shutdown();
        waitLock.lock();
        try {
            waitCondition.signalAll();
        } finally {
            waitLock.unlock();
        }

        // Interrupt executor thread
        if (executorThread != null && executorThread.isAlive()) {
            executorThread.interrupt();
            try {
                executorThread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for executor thread to stop");
            }
        }

        log.info("Parent cycle executor stopped");
    }

    /**
     * Main parent cycle execution loop.
     */
    private void executeParentCycle() {
        log.info("Parent cycle executor thread started");

        try {
            // Start in WAITING state
            waitingManager.enterWaiting(context);

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for timer or explicit transition
                    waitLock.lock();
                    try {
                        while (waitingManager.isWaiting() && running.get() && !Thread.currentThread().isInterrupted()) {
                            waitCondition.await();
                        }
                    } finally {
                        waitLock.unlock();
                    }

                    if (!running.get() || Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    // Exit WAITING state
                    waitingManager.exitWaiting();

                    // Mark registry as active
                    if (registry instanceof org.sokybot.engine.core.workflow.WorkflowRegistryImpl) {
                        ((org.sokybot.engine.core.workflow.WorkflowRegistryImpl) registry).setCycleActive(true);
                    }

                    // Execute workflow components in order
                    executeWorkflowComponents();

                    // Mark registry as inactive
                    if (registry instanceof org.sokybot.engine.core.workflow.WorkflowRegistryImpl) {
                        ((org.sokybot.engine.core.workflow.WorkflowRegistryImpl) registry).setCycleActive(false);
                    }

                    // Return to WAITING state
                    waitingManager.enterWaiting(context);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("Parent cycle executor interrupted");
                    break;
                } catch (Throwable t) {
                    log.error("Fatal error in parent cycle execution: {}", t.getMessage(), t);
                    if (t instanceof Error) {
                        throw (Error) t;
                    }
                    // Return to WAITING state on error
                    waitingManager.enterWaiting(context);
                }
            }
        } finally {
            log.info("Parent cycle executor thread stopped");
        }
    }

    /**
     * Executes all workflow components in priority order.
     */
    private void executeWorkflowComponents() {
        List<String> components = registry.getOrderedWorkflowComponents();

        log.debug("Executing {} workflow components", components.size());

        for (String componentName : components) {
            if (!running.get() || Thread.currentThread().isInterrupted()) {
                break;
            }

            try {
                // Check if component is a cycle first
                ICycleDefinition cycle = registry.getCycle(componentName);
                if (cycle != null && cycle.isEnabled()) {
                    // Execute cycle
                    executeCycle(cycle);
                    continue;
                }

                // Check if component is an orthogonal state
                IWorkflowState state = registry.getState(componentName);
                if (state != null && state instanceof IOrthogonalState) {
                    // Execute orthogonal state
                    executeOrthogonalState((IOrthogonalState) state);
                    continue;
                }

                log.warn("Workflow component '{}' not found or disabled", componentName);

            } catch (Exception e) {
                log.error("Error executing workflow component '{}': {}",
                        componentName, e.getMessage(), e);
                // Continue with next component
            }
        }
    }

    /**
     * Executes an orthogonal state.
     */
    private void executeOrthogonalState(IOrthogonalState state) {
        if (state == null) {
            return;
        }

        log.debug("Executing orthogonal state: {}", state.getName());

        // Update context current state
        if (context instanceof WorkflowContextImpl) {
            ((WorkflowContextImpl) context).setCurrentStateName(state.getName());
        }

        // Evaluate guard
        boolean guardPassed = true;
        if (state.getGuard() != null) {
            try {
                guardPassed = state.getGuard().evaluate(context);
            } catch (WorkflowException e) {
                log.error("Error evaluating guard for state '{}': {}",
                        state.getName(), e.getMessage(), e);
                guardPassed = false;
            }
        }

        if (!guardPassed) {
            // Guard failed: move to next component (no action)
            log.debug("Guard failed for state '{}', moving to next component", state.getName());
            return;
        }

        // Guard passed: execute action
        if (state.getAction() != null) {
            try {
                state.getAction().execute(context);
            } catch (WorkflowException e) {
                log.error("Error executing action for state '{}': {}",
                        state.getName(), e.getMessage(), e);
            }
        }

        // Apply delay if custom delay set
        int delay = 500; // Default delay
        if (state.getCustomDelay() != null && state.getCustomDelay() > 0) {
            delay = Math.max(state.getCustomDelay(), 10);
        }

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay interrupted for state '{}'", state.getName());
        }

        // Process action queue
        queueProcessor.processQueue(context);

        // State complete, move to next component (return to WAITING after all
        // components)
    }

    /**
     * Executes a cycle.
     */
    private void executeCycle(ICycleDefinition cycle) {
        if (cycle == null || !cycle.isEnabled()) {
            return;
        }

        log.debug("Executing cycle: {}", cycle.getName());

        try {
            // Execute cycle
            boolean completed = cycleExecutor.executeCycle(cycle);

            if (completed) {
                log.debug("Cycle '{}' completed", cycle.getName());
            } else {
                log.debug("Cycle '{}' exited early or was interrupted", cycle.getName());
            }
        } catch (Exception e) {
            log.error("Uncaught error executing cycle '{}': {}", cycle.getName(), e.getMessage(), e);
        }
    }

    /**
     * Checks if the executor is running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Triggers transition from WAITING state.
     * Called by WaitingStateManager when timer expires.
     */
    public void triggerTransition() {
        waitLock.lock();
        try {
            waitCondition.signalAll();
        } finally {
            waitLock.unlock();
        }
    }
}
