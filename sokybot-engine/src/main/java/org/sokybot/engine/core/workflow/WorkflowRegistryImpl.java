package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Implementation of workflow registry.
 * Manages cycle registration, ordering, and state lookups.
 */
public class WorkflowRegistryImpl implements IWorkflowRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowRegistryImpl.class);
    
    // Orthogonal states (single states in overall cycle)
    private final Map<String, IOrthogonalState> orthogonalStates = new ConcurrentHashMap<>();
    private final List<PriorityStateEntry> orthogonalStateOrder = new ArrayList<>();
    
    // Cycles
    private final Map<String, ICycleDefinition> cycles = new ConcurrentHashMap<>();
    private final List<PriorityCycleEntry> cycleOrder = new ArrayList<>();
    
    // All states (fully qualified names: "cycle.state" or just "state")
    private final Map<String, IWorkflowState> allStates = new ConcurrentHashMap<>();
    
    private volatile boolean cycleActive = false;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Helper classes for priority ordering
    private static class PriorityStateEntry implements Comparable<PriorityStateEntry> {
        final String stateName;
        final int priority;
        
        PriorityStateEntry(String stateName, int priority) {
            this.stateName = stateName;
            this.priority = priority;
        }
        
        @Override
        public int compareTo(PriorityStateEntry other) {
            return Integer.compare(this.priority, other.priority);
        }
    }
    
    private static class PriorityCycleEntry implements Comparable<PriorityCycleEntry> {
        final String cycleName;
        final int priority;
        
        PriorityCycleEntry(String cycleName, int priority) {
            this.cycleName = cycleName;
            this.priority = priority;
        }
        
        @Override
        public int compareTo(PriorityCycleEntry other) {
            return Integer.compare(this.priority, other.priority);
        }
    }
    
    @Override
    public int registerOrthogonalState(IOrthogonalState state) {
        lock.writeLock().lock();
        try {
            if (cycleActive) {
                throw new IllegalStateException("Cannot register state while cycle is active");
            }
            
            if (state == null) {
                throw new IllegalArgumentException("State cannot be null");
            }
            
            String stateName = state.getName();
            if (stateName == null || stateName.trim().isEmpty()) {
                throw new IllegalArgumentException("State name cannot be null or empty");
            }
            
            if (orthogonalStates.containsKey(stateName)) {
                throw new IllegalArgumentException("State already registered: " + stateName);
            }
            
            // Check for conflicts with cycle state names
            if (allStates.containsKey(stateName)) {
                throw new IllegalArgumentException("State name conflicts with existing state: " + stateName);
            }
            
            int desiredPriority = state.getDesiredPriority();
            int actualPriority = resolvePriority(desiredPriority, orthogonalStateOrder);
            
            orthogonalStates.put(stateName, state);
            orthogonalStateOrder.add(new PriorityStateEntry(stateName, actualPriority));
            Collections.sort(orthogonalStateOrder);
            
            allStates.put(stateName, state);
            
            log.debug("Registered orthogonal state: {} with priority {}", stateName, actualPriority);
            
            return actualPriority;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int registerAfter(IOrthogonalState state, String afterState, int fallbackPriority) {
        lock.writeLock().lock();
        try {
            if (cycleActive) {
                throw new IllegalStateException("Cannot register state while cycle is active");
            }
            
            // Find position of afterState
            int afterIndex = -1;
            for (int i = 0; i < orthogonalStateOrder.size(); i++) {
                if (orthogonalStateOrder.get(i).stateName.equals(afterState)) {
                    afterIndex = i;
                    break;
                }
            }
            
            int actualPriority;
            if (afterIndex >= 0) {
                // Place after the found state
                int afterPriority = orthogonalStateOrder.get(afterIndex).priority;
                actualPriority = afterPriority + 1;
            } else {
                // Use fallback priority
                log.warn("After state '{}' not found, using fallback priority {}", afterState, fallbackPriority);
                actualPriority = resolvePriority(fallbackPriority, orthogonalStateOrder);
            }
            
            // Register with resolved priority
            String stateName = state.getName();
            orthogonalStates.put(stateName, state);
            orthogonalStateOrder.add(new PriorityStateEntry(stateName, actualPriority));
            Collections.sort(orthogonalStateOrder);
            allStates.put(stateName, state);
            
            return actualPriority;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean unregisterOrthogonalState(String stateName) {
        lock.writeLock().lock();
        try {
            if (cycleActive) {
                throw new IllegalStateException("Cannot unregister state while cycle is active");
            }
            
            if (orthogonalStates.remove(stateName) != null) {
                orthogonalStateOrder.removeIf(entry -> entry.stateName.equals(stateName));
                allStates.remove(stateName);
                log.debug("Unregistered orthogonal state: {}", stateName);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int registerCycle(ICycleDefinition cycle) {
        lock.writeLock().lock();
        try {
            if (cycleActive) {
                throw new IllegalStateException("Cannot register cycle while cycle is active");
            }
            
            if (cycle == null) {
                throw new IllegalArgumentException("Cycle cannot be null");
            }
            
            String cycleName = cycle.getName();
            if (cycleName == null || cycleName.trim().isEmpty()) {
                throw new IllegalArgumentException("Cycle name cannot be null or empty");
            }
            
            if (cycles.containsKey(cycleName)) {
                throw new IllegalArgumentException("Cycle already registered: " + cycleName);
            }
            
            // Validate cycle
            validateCycle(cycle);
            
            // Register cycle
            int desiredPriority = cycle.getDesiredPriority();
            int actualPriority = resolvePriority(desiredPriority, cycleOrder);
            
            cycles.put(cycleName, cycle);
            cycleOrder.add(new PriorityCycleEntry(cycleName, actualPriority));
            Collections.sort(cycleOrder);
            
            // Register cycle states with fully qualified names
            for (ICycleState state : cycle.getStates()) {
                String qualifiedName = cycleName + "." + state.getName();
                allStates.put(qualifiedName, state);
            }
            
            log.debug("Registered cycle: {} with priority {}", cycleName, actualPriority);
            
            return actualPriority;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void validateCycle(ICycleDefinition cycle) {
        // Validate entry state exists
        String entryStateName = cycle.getEntryStateName();
        boolean entryStateFound = cycle.getStates().stream()
                .anyMatch(s -> s.getName().equals(entryStateName));
        
        if (!entryStateFound) {
            throw new IllegalArgumentException("Entry state '" + entryStateName + "' not found in cycle '" + cycle.getName() + "'");
        }
        
        // Validate state references
        for (ICycleState state : cycle.getStates()) {
            String stateName = state.getName();
            
            // Validate nextState
            String nextState = state.getNextState();
            if (nextState != null) {
                boolean nextStateFound = cycle.getStates().stream()
                        .anyMatch(s -> s.getName().equals(nextState));
                if (!nextStateFound) {
                    log.warn("State '{}' references nextState '{}' which doesn't exist in cycle '{}'", 
                            stateName, nextState, cycle.getName());
                }
            }
            
            // Validate targetState
            String targetState = state.getTargetState();
            if (targetState != null) {
                boolean targetStateFound = cycle.getStates().stream()
                        .anyMatch(s -> s.getName().equals(targetState));
                if (!targetStateFound) {
                    log.warn("State '{}' references targetState '{}' which doesn't exist in cycle '{}'", 
                            stateName, targetState, cycle.getName());
                }
            }
        }
    }
    
    @Override
    public boolean unregisterCycle(String cycleName) {
        lock.writeLock().lock();
        try {
            if (cycleActive) {
                throw new IllegalStateException("Cannot unregister cycle while cycle is active");
            }
            
            ICycleDefinition cycle = cycles.remove(cycleName);
            if (cycle != null) {
                cycleOrder.removeIf(entry -> entry.cycleName.equals(cycleName));
                
                // Remove cycle states
                for (ICycleState state : cycle.getStates()) {
                    String qualifiedName = cycleName + "." + state.getName();
                    allStates.remove(qualifiedName);
                }
                
                log.debug("Unregistered cycle: {}", cycleName);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void setCycleEnabled(String cycleName, boolean enabled) {
        lock.writeLock().lock();
        try {
            ICycleDefinition cycle = cycles.get(cycleName);
            if (cycle == null) {
                throw new IllegalArgumentException("Cycle not found: " + cycleName);
            }
            if (cycle instanceof CycleDefinitionImpl) {
                ((CycleDefinitionImpl) cycle).setEnabled(enabled);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean isCycleEnabled(String cycleName) {
        lock.readLock().lock();
        try {
            ICycleDefinition cycle = cycles.get(cycleName);
            return cycle != null && cycle.isEnabled();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<String> getRegisteredCycles() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(cycles.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public ICycleDefinition getCycle(String cycleName) {
        lock.readLock().lock();
        try {
            return cycles.get(cycleName);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<String> getOrderedWorkflowComponents() {
        lock.readLock().lock();
        try {
            List<String> components = new ArrayList<>();
            
            // Combine orthogonal states and cycles in priority order
            int stateIndex = 0;
            int cycleIndex = 0;
            
            while (stateIndex < orthogonalStateOrder.size() || cycleIndex < cycleOrder.size()) {
                int statePriority = stateIndex < orthogonalStateOrder.size() 
                        ? orthogonalStateOrder.get(stateIndex).priority 
                        : Integer.MAX_VALUE;
                int cyclePriority = cycleIndex < cycleOrder.size() 
                        ? cycleOrder.get(cycleIndex).priority 
                        : Integer.MAX_VALUE;
                
                if (statePriority <= cyclePriority) {
                    components.add(orthogonalStateOrder.get(stateIndex).stateName);
                    stateIndex++;
                } else {
                    components.add(cycleOrder.get(cycleIndex).cycleName);
                    cycleIndex++;
                }
            }
            
            return components;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<String> getAllStateNames() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(allStates.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public IWorkflowState getState(String fullyQualifiedStateName) {
        lock.readLock().lock();
        try {
            // Try fully qualified name first
            IWorkflowState state = allStates.get(fullyQualifiedStateName);
            if (state != null) {
                return state;
            }
            
            // Try as simple name (for orthogonal states)
            return allStates.get(fullyQualifiedStateName);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean isCycleActive() {
        return cycleActive;
    }
    
    public void setCycleActive(boolean active) {
        this.cycleActive = active;
    }
    
    /**
     * Resolves priority, handling conflicts.
     * If desired priority is taken, finds next available slot.
     */
    private int resolvePriority(int desiredPriority, List<? extends Comparable<?>> order) {
        if (order.isEmpty()) {
            return desiredPriority;
        }
        
        // Check if priority is available
        // For simplicity, find next available slot after desired priority
        // More sophisticated conflict resolution could be added
        
        int actualPriority = desiredPriority;
        
        // For cycles, check existing cycle priorities
        if (order.get(0) instanceof PriorityCycleEntry) {
            Set<Integer> usedPriorities = cycleOrder.stream()
                    .map(e -> e.priority)
                    .collect(Collectors.toSet());
            
            while (usedPriorities.contains(actualPriority)) {
                actualPriority++;
                if (actualPriority > 9999) {
                    actualPriority = 9999;
                    break;
                }
            }
        }
        
        // For states, check existing state priorities
        if (order.get(0) instanceof PriorityStateEntry) {
            Set<Integer> usedPriorities = orthogonalStateOrder.stream()
                    .map(e -> e.priority)
                    .collect(Collectors.toSet());
            
            while (usedPriorities.contains(actualPriority)) {
                actualPriority++;
                if (actualPriority > 9999) {
                    actualPriority = 9999;
                    break;
                }
            }
        }
        
        if (actualPriority != desiredPriority) {
            log.warn("Priority conflict: requested {}, using {}", desiredPriority, actualPriority);
        }
        
        return actualPriority;
    }
    
    public List<String> getOrderedStates() {
        lock.readLock().lock();
        try {
            return orthogonalStateOrder.stream()
                    .map(e -> e.stateName)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public IOrthogonalState getOrthogonalState(String stateName) {
        lock.readLock().lock();
        try {
            return orthogonalStates.get(stateName);
        } finally {
            lock.readLock().unlock();
        }
    }
}
