package org.sokybot.engine.core.interruption;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.interruption.CycleStateSaver.SavedState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cycle interruptions based on priority.
 *
 * <h3>Interruption evaluation order</h3>
 * When {@link #checkForInterruption} is called, each registered candidate cycle
 * is tested in the following order. The first candidate to pass all checks wins:
 * <ol>
 *   <li><b>Self-skip:</b> a cycle cannot interrupt itself.</li>
 *   <li><b>Priority gate:</b> candidate {@code interruptionPriority} must exceed
 *       the currently running cycle's priority.</li>
 *   <li><b>Entry guard pre-check:</b> the candidate's {@code entryGuard} (if any)
 *       must pass. A cycle that cannot start on its own cannot interrupt another.</li>
 *   <li><b>Cooldown:</b> the candidate must not have interrupted the same target
 *       within {@link #INTERRUPTION_COOLDOWN_MS}.</li>
 *   <li><b>Interruption guard:</b> the candidate's {@code interruptionGuard}
 *       must evaluate to {@code true}.</li>
 * </ol>
 *
 * <h3>Threading model</h3>
 * All cycle execution (including nested interruptions) runs on a single
 * {@code ParentCycleExecutor} thread per machine. The {@code volatile} fields
 * exist solely so that {@code stop()} from another thread is visible. No
 * external synchronization is required.
 *
 * <h3>Script requirements</h3>
 * Any cycle script that sets an {@code interruptionGuard} should also ensure
 * the guard checks that the game is in an active/in-world state (e.g.
 * {@code isLoggedIn(ctx)}) to avoid preempting the login cycle before the
 * character has spawned.
 */
public class InterruptionManager {
    
    private static final Logger log = LoggerFactory.getLogger(InterruptionManager.class);

    /**
     * Minimum interval between repeated interruptions of the same target by
     * the same interruptor. Prevents ping-pong loops where a cycle interrupts,
     * immediately yields, and interrupts again.
     */
    static final long INTERRUPTION_COOLDOWN_MS = 5000L;
    
    private final IWorkflowRegistry registry;
    private final CycleStateSaver stateSaver;
    
    // Currently executing cycle tracking
    private volatile String currentCycleName;
    private volatile ICycleState currentCycleState;
    private volatile int currentCyclePriority;
    private volatile SavedState savedState;
    
    // Interruption tracking
    private final Map<String, Integer> interruptionCounts = new ConcurrentHashMap<>();

    // Anti-ping-pong: interruptorName -> (targetName -> timestamp of last interruption)
    private final Map<String, Map<String, Long>> interruptionTimestamps = new HashMap<>();
    
    public InterruptionManager(IWorkflowRegistry registry) {
        this.registry = registry;
        this.stateSaver = new CycleStateSaver();
    }
    
    /**
     * Checks if a higher priority cycle needs to interrupt the current cycle.
     * See class Javadoc for the full evaluation order.
     * 
     * @param context The workflow context
     * @return The interrupting cycle, or null if no interruption needed
     */
    public ICycleDefinition checkForInterruption(IWorkflowContext context) {
        if (currentCycleName == null || currentCyclePriority == 0) {
            return null;
        }
        
        long now = System.currentTimeMillis();
        List<String> registeredCycles = registry.getRegisteredCycles();
        
        for (String cycleName : registeredCycles) {
            // 1. Self-skip
            if (cycleName.equals(currentCycleName)) {
                continue;
            }

            ICycleDefinition cycle = registry.getCycle(cycleName);
            if (cycle == null || !cycle.isEnabled()) {
                continue;
            }
            
            // 2. Priority gate
            int interruptionPriority = cycle.getInterruptionPriority();
            if (interruptionPriority <= currentCyclePriority) {
                continue;
            }

            // 3. Entry guard pre-check: cycle must be able to start on its own
            IGuard entryGuard = cycle.getEntryGuard();
            if (entryGuard != null) {
                try {
                    if (!entryGuard.evaluate(context)) {
                        log.debug("Interruption candidate {} skipped: entry guard failed", cycleName);
                        continue;
                    }
                } catch (Exception e) {
                    log.debug("Interruption candidate {} skipped: entry guard error: {}",
                            cycleName, e.getMessage());
                    continue;
                }
            }

            // 4. Cooldown: prevent repeated interruptions of the same target
            Map<String, Long> targetTimestamps = interruptionTimestamps
                    .getOrDefault(cycleName, Collections.emptyMap());
            Long lastInterrupt = targetTimestamps.get(currentCycleName);
            if (lastInterrupt != null && (now - lastInterrupt) < INTERRUPTION_COOLDOWN_MS) {
                log.debug("Interruption candidate {} in cooldown for target {} ({} ms remaining)",
                        cycleName, currentCycleName,
                        INTERRUPTION_COOLDOWN_MS - (now - lastInterrupt));
                continue;
            }

            // 5. Interruption guard
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
            return null;
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
        
        // Track interruption count
        interruptionCounts.merge(interruptingCycle.getName(), 1, Integer::sum);

        // Record timestamp for anti-ping-pong cooldown
        interruptionTimestamps
                .computeIfAbsent(interruptingCycle.getName(), k -> new HashMap<>())
                .put(currentCycleName, System.currentTimeMillis());
        
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
     * Clears interruption statistics and cooldown timestamps.
     */
    public void clearStats() {
        interruptionCounts.clear();
        interruptionTimestamps.clear();
    }
}
