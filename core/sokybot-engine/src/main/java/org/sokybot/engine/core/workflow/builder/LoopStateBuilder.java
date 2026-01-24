package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.LoopStateImpl;

/**
 * Builder for loop states.
 */
public class LoopStateBuilder {
    private final String name;
    private IGuard guard;
    private IAction action;
    private String nextState;
    private String targetState;
    private Integer customDelay;
    private IStateLifecycle lifecycle;
    
    private String loopBackState;
    private IGuard loopGuard;
    private Integer maxIterations;
    private Integer loopDelay;
    private String counterKey;
    private IAction loopExitAction;
    
    public LoopStateBuilder(String name) {
        this.name = name;
    }
    
    public LoopStateBuilder guard(IGuard guard) {
        this.guard = guard;
        return this;
    }
    
    public LoopStateBuilder action(IAction action) {
        this.action = action;
        return this;
    }
    
    public LoopStateBuilder nextState(String stateName) {
        this.nextState = stateName;
        return this;
    }
    
    public LoopStateBuilder targetState(String stateName) {
        this.targetState = stateName;
        return this;
    }
    
    public LoopStateBuilder delay(int ms) {
        this.customDelay = ms;
        return this;
    }
    
    public LoopStateBuilder lifecycle(IStateLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    public LoopStateBuilder loopBackTo(String stateName) {
        this.loopBackState = stateName;
        return this;
    }
    
    public LoopStateBuilder whileGuard(IGuard guard) {
        this.loopGuard = guard;
        return this;
    }
    
    public LoopStateBuilder maxIterations(int max) {
        this.maxIterations = max;
        return this;
    }
    
    public LoopStateBuilder loopDelay(int ms) {
        this.loopDelay = ms;
        return this;
    }
    
    public LoopStateBuilder counterKey(String key) {
        this.counterKey = key;
        return this;
    }
    
    public LoopStateBuilder onLoopExit(IAction action) {
        this.loopExitAction = action;
        return this;
    }
    
    public ILoopState build() {
        if (loopBackState == null) {
            throw new IllegalStateException("loopBackState is required for loop state");
        }
        return new LoopStateImpl(name, guard, action, nextState, targetState,
                                customDelay, lifecycle, loopBackState, loopGuard,
                                maxIterations, loopDelay, counterKey, loopExitAction);
    }
}
