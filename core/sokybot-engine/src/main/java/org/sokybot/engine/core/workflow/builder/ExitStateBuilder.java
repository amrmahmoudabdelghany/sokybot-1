package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.ExitStateImpl;

/**
 * Builder for exit states.
 */
public class ExitStateBuilder {
    private final String name;
    private IGuard guard;
    private IAction action;
    private String nextState;
    private String targetState;
    private Integer customDelay;
    private IStateLifecycle lifecycle;
    
    private IGuard exitGuard;
    private IAction exitAction;
    private String continueState;
    
    public ExitStateBuilder(String name) {
        this.name = name;
    }
    
    public ExitStateBuilder guard(IGuard guard) {
        this.guard = guard;
        return this;
    }
    
    public ExitStateBuilder action(IAction action) {
        this.action = action;
        return this;
    }
    
    public ExitStateBuilder nextState(String stateName) {
        this.nextState = stateName;
        return this;
    }
    
    public ExitStateBuilder targetState(String stateName) {
        this.targetState = stateName;
        return this;
    }
    
    public ExitStateBuilder delay(int ms) {
        this.customDelay = ms;
        return this;
    }
    
    public ExitStateBuilder lifecycle(IStateLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    public ExitStateBuilder exitWhen(IGuard guard) {
        this.exitGuard = guard;
        return this;
    }
    
    public ExitStateBuilder beforeExit(IAction action) {
        this.exitAction = action;
        return this;
    }
    
    public ExitStateBuilder orContinueTo(String stateName) {
        this.continueState = stateName;
        return this;
    }
    
    public IExitState build() {
        return new ExitStateImpl(name, guard, action, nextState, targetState,
                                customDelay, lifecycle, exitGuard, exitAction, continueState);
    }
}
