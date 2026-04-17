package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of cycle definition.
 * Manages cycle states and provides cycle operations.
 */
public class CycleDefinitionImpl implements ICycleDefinition {
    
    private final CycleId name;
    private final int desiredPriority;
    private final StateId entryStateName;
    private final IGuard entryGuard;
    private final IGuard interruptionGuard;
    private final int interruptionPriority;
    private final IAction interruptionAction;
    private final boolean interruptible;
    private final ICycleLifecycle lifecycle;
    private volatile boolean enabled;
    
    private final Map<StateId, ICycleState> states = new ConcurrentHashMap<>();
    private final List<StateId> stateOrder = new ArrayList<>();
    
    public CycleDefinitionImpl(String name, int desiredPriority, String entryStateName,
                              IGuard entryGuard, IGuard interruptionGuard,
                              int interruptionPriority, IAction interruptionAction,
                              boolean interruptible, ICycleLifecycle lifecycle,
                              List<ICycleState> states) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cycle name cannot be null or empty");
        }
        if (entryStateName == null || entryStateName.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry state name cannot be null or empty");
        }
        
        this.name = CycleId.of(name);
        this.desiredPriority = desiredPriority;
        this.entryStateName = StateId.of(entryStateName);
        this.entryGuard = entryGuard;
        this.interruptionGuard = interruptionGuard;
        this.interruptionPriority = interruptionPriority;
        this.interruptionAction = interruptionAction;
        this.interruptible = interruptible;
        this.lifecycle = lifecycle;
        this.enabled = true;
        
        // Register states
        if (states != null) {
            for (ICycleState state : states) {
                registerState(state);
            }
        }
        
        // Validate entry state exists
        if (!this.states.containsKey(this.entryStateName)) {
            throw new IllegalArgumentException("Entry state '" + entryStateName + "' not found in cycle '" + name + "'");
        }
        
        // Validate state references
        validateStateReferences();
    }
    
    private void registerState(ICycleState state) {
        if (states.containsKey(state.getStateId())) {
            throw new IllegalArgumentException("Duplicate state name: " + state.getStateId());
        }
        states.put(state.getStateId(), state);
        stateOrder.add(state.getStateId());
    }
    
    private void validateStateReferences() {
        for (ICycleState state : states.values()) {
            // Validate nextState
            StateId nextState = state.getNextState();
            if (nextState != null && !states.containsKey(nextState)) {
                // Could be a loop back - check if it's intentional
                // For now, just log warning
            }
            
            // Validate targetState
            StateId targetState = state.getTargetState();
            if (targetState != null && !states.containsKey(targetState)) {
                // Could be a loop back - check if it's intentional
                // For now, just log warning
            }
            
            // Validate special state references
            if (state.getStateType() == StateType.LOOP && state instanceof ILoopState) {
                ILoopState loopState = (ILoopState) state;
                StateId loopBackState = loopState.getLoopBackState();
                if (loopBackState != null && !states.containsKey(loopBackState)) {
                    throw new IllegalArgumentException("Loop back state '" + loopBackState + 
                                                     "' not found for state '" + state.getName() + "'");
                }
            }
        }
    }
    
    @Override
    public CycleId getCycleId() {
        return name;
    }
    
    @Override
    public int getDesiredPriority() {
        return desiredPriority;
    }
    
    @Override
    public StateId getEntryState() {
        return entryStateName;
    }
    
    @Override
    public List<ICycleState> getStates() {
        return stateOrder.stream()
                .map(states::get)
                .collect(Collectors.toList());
    }
    
    @Override
    public IGuard getEntryGuard() {
        return entryGuard;
    }
    
    @Override
    public IGuard getInterruptionGuard() {
        return interruptionGuard;
    }
    
    @Override
    public int getInterruptionPriority() {
        return interruptionPriority;
    }
    
    @Override
    public IAction getInterruptionAction() {
        return interruptionAction;
    }
    
    @Override
    public boolean isInterruptible() {
        return interruptible;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public ICycleState getState(StateId stateName) {
        return states.get(stateName);
    }
    
    @Override
    public ICycleLifecycle getLifecycle() {
        return lifecycle;
    }
}
