package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.DelayStateImpl;

/**
 * Builder for delay states.
 */
public class DelayStateBuilder {
    private final String name;
    private IGuard guard;
    private IAction action;
    private String nextState;
    private String targetState;
    private Integer customDelay;
    private IStateLifecycle lifecycle;
    
    private int delayMs;
    private int minDelayMs = 0;
    private IAction delayAction;
    
    public DelayStateBuilder(String name) {
        this.name = name;
    }
    
    public DelayStateBuilder guard(IGuard guard) {
        this.guard = guard;
        return this;
    }
    
    public DelayStateBuilder action(IAction action) {
        this.action = action;
        return this;
    }
    
    public DelayStateBuilder nextState(String stateName) {
        this.nextState = stateName;
        return this;
    }
    public DelayStateBuilder nextState(StateId stateId) {
        this.nextState = stateId != null ? stateId.asString() : null;
        return this;
    }
    
    public DelayStateBuilder targetState(String stateName) {
        this.targetState = stateName;
        return this;
    }
    public DelayStateBuilder targetState(StateId stateId) {
        this.targetState = stateId != null ? stateId.asString() : null;
        return this;
    }
    
    public DelayStateBuilder delay(int ms) {
        this.delayMs = ms;
        return this;
    }
    
    public DelayStateBuilder minDelay(int ms) {
        this.minDelayMs = ms;
        return this;
    }
    
    public DelayStateBuilder delayAction(IAction action) {
        this.delayAction = action;
        return this;
    }
    
    public DelayStateBuilder lifecycle(IStateLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    public IDelayState build() {
        return new DelayStateImpl(name, guard, action, nextState, targetState,
                                 customDelay, lifecycle, delayMs, minDelayMs, delayAction);
    }
}
