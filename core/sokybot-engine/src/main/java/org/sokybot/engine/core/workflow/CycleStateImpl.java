package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

/**
 * Implementation of a cycle state.
 * Standard state with guard, action, and transitions.
 */
public class CycleStateImpl implements ICycleState {
    
    private final StateId name;
    private final IGuard guard;
    private final IAction action;
    private final StateId nextState;
    private final StateId targetState;
    private final Integer customDelay;
    private final IStateLifecycle lifecycle;
    private final StateType stateType;
    
    public CycleStateImpl(String name, IGuard guard, IAction action, 
                         String nextState, String targetState, 
                         Integer customDelay, IStateLifecycle lifecycle,
                         StateType stateType) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("State name cannot be null or empty");
        }
        this.name = StateId.of(name);
        this.guard = guard;
        this.action = action;
        this.nextState = StateId.ofNullable(nextState);
        this.targetState = StateId.ofNullable(targetState);
        this.customDelay = customDelay;
        this.lifecycle = lifecycle;
        this.stateType = stateType != null ? stateType : StateType.STANDARD;
    }
    
    @Override
    public StateId getStateId() {
        return name;
    }
    
    @Override
    public IGuard getGuard() {
        return guard;
    }
    
    @Override
    public IAction getAction() {
        return action;
    }
    
    @Override
    public StateId getNextState() {
        return nextState;
    }
    
    @Override
    public StateId getTargetState() {
        return targetState;
    }
    
    @Override
    public Integer getCustomDelay() {
        return customDelay;
    }
    
    @Override
    public IStateLifecycle getLifecycle() {
        return lifecycle;
    }
    
    @Override
    public StateType getStateType() {
        return stateType;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends IWorkflowState> T getAs(Class<T> type) {
        if (type.isInstance(this)) {
            return (T) this;
        }
        return null;
    }
}
