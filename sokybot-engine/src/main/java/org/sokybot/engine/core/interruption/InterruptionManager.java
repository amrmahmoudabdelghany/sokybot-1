package org.sokybot.engine.core.interruption;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.interruption.CycleStateSaver.SavedState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cycle interruptions based on priority.
 * Tracks currently executing cycle and checks for interrupting cycles.
 */
public class InterruptionManager {
    
    private static final Logger log = LoggerFactory.getLogger(InterruptionManager.class);
    
    private final IWorkflowRegistry registry;
    private final CycleStateSaver stateSaver;
    
    // Currently executing cycle tracking
    private volatile String currentCycleName;
    private volatile ICycleState currentCycleState;
    private volatile int currentCyclePriority;
    private volatile SavedState savedState;
    
    // Interruption tracking
    private final Map<String, Integer> interruptionCounts = new ConcurrentHashMap<>();
    
    public InterruptionManager(IWorkflowRegistry registry) {
        this.registry = registry;
        this.stateSaver = new CycleStateSaver();
    }
    
    /**
     * Checks if a higher priority cycle needs to interrupt the current cycle.
     * 
     * @param context The workflow context
     * @return The interrupting cycle, or null if no interruption needed
     */
    public ICycleDefinition checkForInterruption(IWorkflowContext context) {
        if (currentCycleName == null || currentCyclePriority == 0) {
            return null; // No cycle currently executing
        }
        
        List<String> registeredCycles = registry.getRegisteredCycles();
        
        for (String cycleName : registeredCycles) {
            ICycleDefinition cycle = registry.getCycle(cycleName);
            
            if (cycle == null || !cycle.isEnabled()) {
                continue;
            }
            
            // Check if this cycle has higher interruption priority
            int interruptionPriority = cycle.getInterruptionPriority();
            if (interruptionPriority > currentCyclePriority) {
                // Check interruption guard
                IGuard interruptionGuard = cycle.getInterruptionGuard();
                if (interruptionGuard != null) {
                    try {
                        if (interruptionGuard.evaluate(context)) {
                            log.info("Interruption detected: {} (priority {}) interrupting {} (priority {})",
                                    cycleName, interruptionPriority, currentCycleName, currentCyclePriority);
                            return cycle;
                        }
                    } catch (Exception e) {
                        log.error("Error evaluating interruption guard for cycle {}: {}",
                                cycleName, e.getMessage(), e);
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Interrupts the current cycle.
     * Saves the current state before interruption.
     * 
     * @param interruptingCycle The cycle that is interrupting
     * @param context The workflow context
     * @return The saved state for later restoration
     */
    public SavedState interruptCurrentCycle(ICycleDefinition interruptingCycle, IWorkflowContext context) {
        if (currentCycleName == null || currentCycleState == null) {
            return null; // Nothing to interrupt
        }
        
        log.info("Interrupting cycle {} (state: {}) with cycle {}",
                currentCycleName, currentCycleState.getName(), interruptingCycle.getName());
        
        // Save current state
        savedState = stateSaver.saveState(currentCycleName, currentCycleState, context);
        
        // Execute interruption action if defined
        IAction interruptionAction = interruptingCycle.getInterruptionAction();
        if (interruptionAction != null) {
            try {
                interruptionAction.execute(context);
            } catch (Exception e) {
                log.error("Error executing interruption action for cycle {}: {}",
                        interruptingCycle.getName(), e.getMessage(), e);
            }
        }
        
        // Track interruption
        interruptionCounts.merge(interruptingCycle.getName(), 1, Integer::sum);
        
        return savedState;
    }
    
    /**
     * Resumes an interrupted cycle.
     * Restores the saved state.
     * 
     * @param savedState The saved state to restore
     * @param context The workflow context
     * @return The cycle state to resume from, or null if cannot resume
     */
    public ICycleState resumeCycle(SavedState savedState, IWorkflowContext context) {
        if (savedState == null) {
            return null;
        }
        
        ICycleDefinition cycle = registry.getCycle(savedState.getCycleName());
        if (cycle == null) {
            log.warn("Cannot resume cycle {}: cycle not found", savedState.getCycleName());
            return null;
        }
        
        if (!cycle.isInterruptible()) {
            log.warn("Cannot resume cycle {}: cycle is not interruptible", savedState.getCycleName());
            return null;
        }
        
        log.info("Resuming cycle {} from state {}", savedState.getCycleName(), savedState.getStateName());
        
        // Restore state
        stateSaver.restoreState(savedState, context);
        
        // Get the state to resume from
        ICycleState state = cycle.getState(savedState.getStateName());
        if (state == null) {
            // Entry state not found, use entry state
            state = cycle.getState(cycle.getEntryStateName());
        }
        
        return state;
    }
    
    /**
     * Sets the currently executing cycle.
     * Called when a cycle starts executing.
     */
    public void setCurrentCycle(String cycleName, ICycleState cycleState, int priority) {
        this.currentCycleName = cycleName;
        this.currentCycleState = cycleState;
        this.currentCyclePriority = priority;
        this.savedState = null;
    }
    
    /**
     * Clears the currently executing cycle.
     * Called when a cycle completes or is stopped.
     */
    public void clearCurrentCycle() {
        if (savedState != null) {
            stateSaver.clearSavedState(savedState);
            savedState = null;
        }
        this.currentCycleName = null;
        this.currentCycleState = null;
        this.currentCyclePriority = 0;
    }
    
    /**
     * Gets the currently executing cycle name.
     * 
     * @return The cycle name, or null if no cycle executing
     */
    public String getCurrentCycleName() {
        return currentCycleName;
    }
    
    /**
     * Gets interruption statistics.
     * 
     * @return Map of cycle name to interruption count
     */
    public Map<String, Integer> getInterruptionStats() {
        return new HashMap<>(interruptionCounts);
    }
    
    /**
     * Clears interruption statistics.
     */
    public void clearStats() {
        interruptionCounts.clear();
    }
}
