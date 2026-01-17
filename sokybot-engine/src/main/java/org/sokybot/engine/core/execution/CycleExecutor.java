package org.sokybot.engine.core.execution;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.interruption.CycleStateSaver.SavedState;
import org.sokybot.engine.core.interruption.InterruptionManager;
import org.sokybot.engine.core.queue.ActionQueueProcessorImpl;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes individual cycles.
 * Handles state transitions, guards, actions, and special states.
 */
public class CycleExecutor {
    
    private static final Logger log = LoggerFactory.getLogger(CycleExecutor.class);
    private static final long DEFAULT_DELAY_MS = 500;
    private static final int MIN_DELAY_MS = 10; // Minimum delay to prevent busy-wait
    
    private final InterruptionManager interruptionManager;
    private final ActionQueueProcessorImpl queueProcessor;
    private final IWorkflowContext context;
    
    public CycleExecutor(InterruptionManager interruptionManager,
                        ActionQueueProcessorImpl queueProcessor,
                        IWorkflowContext context) {
        this.interruptionManager = interruptionManager;
        this.queueProcessor = queueProcessor;
        this.context = context;
    }
    
    /**
     * Executes a cycle from the entry state.
     * 
     * @param cycle The cycle to execute
     * @return true if cycle completed, false if interrupted or exited early
     */
    public boolean executeCycle(ICycleDefinition cycle) {
        if (cycle == null || !cycle.isEnabled()) {
            return false;
        }
        
        String entryStateName = cycle.getEntryStateName();
        ICycleState entryState = cycle.getState(entryStateName);
        
        if (entryState == null) {
            log.error("Entry state '{}' not found in cycle '{}'", entryStateName, cycle.getName());
            return false;
        }
        
        // Check entry guard
        IGuard entryGuard = cycle.getEntryGuard();
        if (entryGuard != null) {
            try {
                if (!entryGuard.evaluate(context)) {
                    log.debug("Entry guard failed for cycle '{}', skipping", cycle.getName());
                    return false;
                }
            } catch (WorkflowException e) {
                log.error("Error evaluating entry guard for cycle '{}': {}", cycle.getName(), e.getMessage(), e);
                return false;
            }
        }
        
        // Execute cycle lifecycle onEnter
        ICycleLifecycle lifecycle = cycle.getLifecycle();
        if (lifecycle != null) {
            try {
                lifecycle.onCycleEnter(context);
            } catch (Exception e) {
                log.error("Error in cycle lifecycle onEnter for cycle '{}': {}", 
                         cycle.getName(), e.getMessage(), e);
            }
        }
        
        // Track current cycle for interruption
        interruptionManager.setCurrentCycle(cycle.getName(), entryState, cycle.getDesiredPriority());
        
        try {
            // Execute cycle
            executeCycleInternal(cycle, entryState);
            
            // Execute cycle lifecycle onExit
            if (lifecycle != null) {
                try {
                    lifecycle.onCycleExit(context);
                } catch (Exception e) {
                    log.error("Error in cycle lifecycle onExit for cycle '{}': {}", 
                             cycle.getName(), e.getMessage(), e);
                }
            }
            
            return true;
            
        } catch (CycleInterruptedException e) {
            // Cycle was interrupted
            log.info("Cycle '{}' was interrupted by cycle '{}'", cycle.getName(), e.getInterruptingCycle());
            
            // Cycle lifecycle onExit not called on interruption (design decision)
            return false;
            
        } finally {
            // Clear current cycle tracking
            interruptionManager.clearCurrentCycle();
        }
    }
    
    /**
     * Internal cycle execution loop.
     */
    private void executeCycleInternal(ICycleDefinition cycle, ICycleState currentState) 
            throws CycleInterruptedException {
        
        Map<String, Integer> stateIterations = new ConcurrentHashMap<>();
        final int MAX_STATE_ITERATIONS = 10000; // Safety limit
        
        while (currentState != null) {
            String stateName = currentState.getName();
            
            // Safety check: prevent infinite loops
            int iterations = stateIterations.merge(stateName, 1, Integer::sum);
            if (iterations > MAX_STATE_ITERATIONS) {
                log.error("State '{}' in cycle '{}' exceeded max iterations ({}), exiting cycle",
                         stateName, cycle.getName(), MAX_STATE_ITERATIONS);
                return;
            }
            
            // Update context current state
            if (context instanceof WorkflowContextImpl) {
                ((WorkflowContextImpl) context).setCurrentStateName(
                    cycle.getName() + "." + stateName);
            }
            
            // Check for interruption
            ICycleDefinition interruptingCycle = interruptionManager.checkForInterruption(context);
            if (interruptingCycle != null && cycle.isInterruptible()) {
                // Save state
                SavedState savedState = interruptionManager.interruptCurrentCycle(interruptingCycle, context);
                
                // Execute interrupting cycle
                CycleExecutor interruptingExecutor = new CycleExecutor(
                    interruptionManager, queueProcessor, context);
                interruptingExecutor.executeCycle(interruptingCycle);
                
                // Resume from saved state
                ICycleState resumedState = interruptionManager.resumeCycle(savedState, context);
                if (resumedState != null) {
                    currentState = resumedState;
                    continue;
                } else {
                    // Cannot resume, exit cycle
                    throw new CycleInterruptedException(interruptingCycle.getName());
                }
            }
            
            // Process action queue before state execution
            queueProcessor.processQueue(context);
            
            // Execute state based on type
            currentState = executeState(cycle, currentState);
            
            // Process action queue after state execution
            queueProcessor.processQueue(context);
        }
    }
    
    /**
     * Executes a single state.
     * 
     * @param cycle The cycle this state belongs to
     * @param state The state to execute
     * @return Next state to execute, or null if cycle should exit
     */
    private ICycleState executeState(ICycleDefinition cycle, ICycleState state) {
        StateType type = state.getStateType();
        
        // State lifecycle onEnter
        IStateLifecycle lifecycle = state.getLifecycle();
        if (lifecycle != null) {
            try {
                lifecycle.onEnter(context);
            } catch (Exception e) {
                log.error("Error in state lifecycle onEnter for state '{}': {}", 
                         state.getName(), e.getMessage(), e);
            }
        }
        
        ICycleState nextState;
        
        try {
            switch (type) {
                case STANDARD:
                    nextState = executeStandardState(cycle, state);
                    break;
                case LOOP:
                    nextState = executeLoopState(cycle, (ILoopState) state);
                    break;
                case DELAY:
                    nextState = executeDelayState(cycle, (IDelayState) state);
                    break;
                case EXIT:
                    nextState = executeExitState(cycle, (IExitState) state);
                    break;
                case CONDITIONAL:
                    nextState = executeConditionalState(cycle, (IConditionalState) state);
                    break;
                case GUARD_ONLY:
                    nextState = executeGuardOnlyState(cycle, (IGuardOnlyState) state);
                    break;
                case RETRY:
                    nextState = executeRetryState(cycle, (IRetryState) state);
                    break;
                default:
                    log.error("Unknown state type: {}", type);
                    return null;
            }
        } finally {
            // State lifecycle onExit
            if (lifecycle != null) {
                try {
                    lifecycle.onExit(context);
                } catch (Exception e) {
                    log.error("Error in state lifecycle onExit for state '{}': {}", 
                             state.getName(), e.getMessage(), e);
                }
            }
        }
        
        return nextState;
    }
    
    /**
     * Executes a standard state.
     */
    private ICycleState executeStandardState(ICycleDefinition cycle, ICycleState state) {
        // Evaluate guard
        boolean guardPassed = true;
        if (state.getGuard() != null) {
            try {
                guardPassed = state.getGuard().evaluate(context);
            } catch (WorkflowException e) {
                log.error("Error evaluating guard for state '{}': {}", 
                         state.getName(), e.getMessage(), e);
                // Treat as guard failure
                guardPassed = false;
            }
        }
        
        String nextStateName;
        long delay = DEFAULT_DELAY_MS;
        
        if (guardPassed) {
            // Guard passed: execute action and use nextState
            if (state.getAction() != null) {
                try {
                    state.getAction().execute(context);
                } catch (WorkflowException e) {
                    log.error("Error executing action for state '{}': {}", 
                             state.getName(), e.getMessage(), e);
                    // Continue despite error
                }
            }
            
            // Apply custom delay if set
            if (state.getCustomDelay() != null && state.getCustomDelay() > 0) {
                delay = Math.max(state.getCustomDelay(), MIN_DELAY_MS);
            } else {
                delay = DEFAULT_DELAY_MS;
            }
            
            nextStateName = state.getNextState();
        } else {
            // Guard failed: use targetState (or exit if null)
            nextStateName = state.getTargetState();
            // Minimum delay even on guard failure to prevent busy-wait
            delay = MIN_DELAY_MS;
        }
        
        // Apply delay
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Delay interrupted for state '{}'", state.getName());
            }
        }
        
        // Transition to next state
        if (nextStateName == null) {
            return null; // Exit cycle
        }
        
        ICycleState nextState = cycle.getState(nextStateName);
        if (nextState == null) {
            log.warn("Next state '{}' not found in cycle '{}', exiting cycle",
                    nextStateName, cycle.getName());
            return null;
        }
        
        return nextState;
    }
    
    /**
     * Executes a loop state.
     */
    private ICycleState executeLoopState(ICycleDefinition cycle, ILoopState loopState) {
        String counterKey = loopState.getCounterKey();
        if (counterKey == null) {
            counterKey = cycle.getName() + "." + loopState.getLoopBackState() + ".iterations";
        }
        
        Map<String, Object> persistentData = context.getPersistentData();
        int iterations = (Integer) persistentData.getOrDefault(counterKey, 0);
        
        // Check max iterations
        if (loopState.getMaxIterations() != null && iterations >= loopState.getMaxIterations()) {
            log.debug("Loop state '{}' reached max iterations ({}), exiting loop",
                     loopState.getName(), loopState.getMaxIterations());
            
            // Execute loop exit action
            if (loopState.getLoopExitAction() != null) {
                try {
                    loopState.getLoopExitAction().execute(context);
                } catch (WorkflowException e) {
                    log.error("Error executing loop exit action: {}", e.getMessage(), e);
                }
            }
            
            persistentData.remove(counterKey);
            return cycle.getState(loopState.getNextState());
        }
        
        // Check loop guard
        if (loopState.getLoopGuard() != null) {
            try {
                if (!loopState.getLoopGuard().evaluate(context)) {
                    // Loop guard fails, exit loop
                    log.debug("Loop guard failed for state '{}', exiting loop", loopState.getName());
                    persistentData.remove(counterKey);
                    return cycle.getState(loopState.getTargetState());
                }
            } catch (WorkflowException e) {
                log.error("Error evaluating loop guard: {}", e.getMessage(), e);
                persistentData.remove(counterKey);
                return cycle.getState(loopState.getTargetState());
            }
        }
        
        // Execute standard guard and action first
        boolean guardPassed = true;
        if (loopState.getGuard() != null) {
            try {
                guardPassed = loopState.getGuard().evaluate(context);
            } catch (WorkflowException e) {
                log.error("Error evaluating guard: {}", e.getMessage(), e);
                guardPassed = false;
            }
        }
        
        if (guardPassed && loopState.getAction() != null) {
            try {
                loopState.getAction().execute(context);
            } catch (WorkflowException e) {
                log.error("Error executing action: {}", e.getMessage(), e);
            }
        }
        
        // Continue loop
        persistentData.put(counterKey, iterations + 1);
        
        // Apply loop delay
        Integer loopDelay = loopState.getLoopDelay();
        if (loopDelay != null && loopDelay > 0) {
            long delay = Math.max(loopDelay, MIN_DELAY_MS);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Loop back
        ICycleState loopBackState = cycle.getState(loopState.getLoopBackState());
        if (loopBackState == null) {
            log.error("Loop back state '{}' not found in cycle '{}'",
                     loopState.getLoopBackState(), cycle.getName());
            return null;
        }
        
        return loopBackState;
    }
    
    /**
     * Executes a delay state.
     */
    private ICycleState executeDelayState(ICycleDefinition cycle, IDelayState delayState) {
        // Execute optional delay action
        if (delayState.getDelayAction() != null) {
            try {
                delayState.getDelayAction().execute(context);
            } catch (WorkflowException e) {
                log.error("Error executing delay action: {}", e.getMessage(), e);
            }
        }
        
        // Apply delay (enforce minimum)
        int delay = Math.max(delayState.getDelayMs(), Math.max(delayState.getMinDelayMs(), MIN_DELAY_MS));
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Delay interrupted for state '{}'", delayState.getName());
        }
        
        return cycle.getState(delayState.getNextState());
    }
    
    /**
     * Executes an exit state.
     */
    private ICycleState executeExitState(ICycleDefinition cycle, IExitState exitState) {
        // Check exit guard
        IGuard exitGuard = exitState.getExitGuard();
        boolean shouldExit = true;
        
        if (exitGuard != null) {
            try {
                shouldExit = exitGuard.evaluate(context);
            } catch (WorkflowException e) {
                log.error("Error evaluating exit guard: {}", e.getMessage(), e);
                shouldExit = true; // Exit on error
            }
        }
        
        if (shouldExit) {
            // Exit cycle
            if (exitState.getExitAction() != null) {
                try {
                    exitState.getExitAction().execute(context);
                } catch (WorkflowException e) {
                    log.error("Error executing exit action: {}", e.getMessage(), e);
                }
            }
            return null; // Exit cycle
        } else {
            // Continue to next state
            String continueState = exitState.getContinueState();
            if (continueState != null) {
                return cycle.getState(continueState);
            } else {
                return cycle.getState(exitState.getNextState());
            }
        }
    }
    
    /**
     * Executes a conditional state.
     */
    private ICycleState executeConditionalState(ICycleDefinition cycle, IConditionalState condState) {
        // Evaluate transitions in order
        for (IConditionalTransition transition : condState.getTransitions()) {
            try {
                if (transition.getGuard().evaluate(context)) {
                    // Match found
                    if (transition.getAction() != null) {
                        try {
                            transition.getAction().execute(context);
                        } catch (WorkflowException e) {
                            log.error("Error executing transition action: {}", e.getMessage(), e);
                        }
                    }
                    return cycle.getState(transition.getTargetState());
                }
            } catch (WorkflowException e) {
                log.error("Error evaluating transition guard: {}", e.getMessage(), e);
                // Continue to next transition
            }
        }
        
        // No match, use default
        return cycle.getState(condState.getDefaultState());
    }
    
    /**
     * Executes a guard-only state.
     */
    private ICycleState executeGuardOnlyState(ICycleDefinition cycle, IGuardOnlyState guardState) {
        try {
            if (guardState.getGuard().evaluate(context)) {
                return cycle.getState(guardState.getSuccessState());
            } else {
                return cycle.getState(guardState.getFailureState());
            }
        } catch (WorkflowException e) {
            log.error("Error evaluating guard: {}", e.getMessage(), e);
            // Treat as failure
            return cycle.getState(guardState.getFailureState());
        }
    }
    
    /**
     * Executes a retry state.
     */
    private ICycleState executeRetryState(ICycleDefinition cycle, IRetryState retryState) {
        String counterKey = cycle.getName() + "." + retryState.getName() + ".retries";
        Map<String, Object> persistentData = context.getPersistentData();
        int retries = (Integer) persistentData.getOrDefault(counterKey, 0);
        
        if (retries >= retryState.getMaxRetries()) {
            // Retries exhausted
            log.debug("Retry state '{}' exhausted after {} retries",
                     retryState.getName(), retryState.getMaxRetries());
            persistentData.remove(counterKey);
            return cycle.getState(retryState.getExhaustedState());
        }
        
        // Check retry guard
        if (retryState.getRetryGuard() != null) {
            try {
                if (!retryState.getRetryGuard().evaluate(context)) {
                    // Should not retry
                    persistentData.remove(counterKey);
                    return cycle.getState(retryState.getExhaustedState());
                }
            } catch (WorkflowException e) {
                log.error("Error evaluating retry guard: {}", e.getMessage(), e);
                persistentData.remove(counterKey);
                return cycle.getState(retryState.getExhaustedState());
            }
        }
        
        // Retry
        persistentData.put(counterKey, retries + 1);
        
        if (retryState.getRetryDelay() != null && retryState.getRetryDelay() > 0) {
            long delay = Math.max(retryState.getRetryDelay(), MIN_DELAY_MS);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        return cycle.getState(retryState.getRetryState());
    }
    
    /**
     * Exception thrown when cycle is interrupted.
     */
    static class CycleInterruptedException extends RuntimeException {
        private final String interruptingCycle;
        
        CycleInterruptedException(String interruptingCycle) {
            super("Cycle interrupted by: " + interruptingCycle);
            this.interruptingCycle = interruptingCycle;
        }
        
        String getInterruptingCycle() {
            return interruptingCycle;
        }
    }
}
