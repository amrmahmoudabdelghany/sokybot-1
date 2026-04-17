package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.CycleStateImpl;

/**
 * Builder for standard cycle states.
 */
public class StateBuilder {
    private final String name;
    private IGuard guard;
    private IAction action;
    private String nextState;
    private String targetState;
    private Integer customDelay;
    private IStateLifecycle lifecycle;
    
    public StateBuilder(String name) {
        this.name = name;
    }
    
    public StateBuilder guard(IGuard guard) {
        this.guard = guard;
        return this;
    }
    
    public StateBuilder action(IAction action) {
        this.action = action;
        return this;
    }
    
    public StateBuilder nextState(String stateName) {
        this.nextState = stateName;
        return this;
    }

    public StateBuilder nextState(StateId stateId) {
        this.nextState = stateId != null ? stateId.asString() : null;
        return this;
    }
    
    public StateBuilder targetState(String stateName) {
        this.targetState = stateName;
        return this;
    }

    public StateBuilder targetState(StateId stateId) {
        this.targetState = stateId != null ? stateId.asString() : null;
        return this;
    }
    
    public StateBuilder delay(int ms) {
        this.customDelay = ms;
        return this;
    }
    
    public StateBuilder lifecycle(IStateLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    public ICycleState build() {
        return new CycleStateImpl(name, guard, action, nextState, targetState, 
                                 customDelay, lifecycle, StateType.STANDARD);
    }
}
