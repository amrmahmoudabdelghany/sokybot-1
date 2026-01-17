package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.ConditionalStateImpl;
import org.sokybot.engine.core.workflow.ConditionalTransitionImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for conditional states.
 */
public class ConditionalStateBuilder {
    private final String name;
    private IGuard guard;
    private IAction action;
    private String nextState;
    private String targetState;
    private Integer customDelay;
    private IStateLifecycle lifecycle;
    
    private final List<IConditionalTransition> transitions = new ArrayList<>();
    private String defaultState;
    
    public ConditionalStateBuilder(String name) {
        this.name = name;
    }
    
    public ConditionalStateBuilder guard(IGuard guard) {
        this.guard = guard;
        return this;
    }
    
    public ConditionalStateBuilder action(IAction action) {
        this.action = action;
        return this;
    }
    
    public ConditionalStateBuilder nextState(String stateName) {
        this.nextState = stateName;
        return this;
    }
    
    public ConditionalStateBuilder targetState(String stateName) {
        this.targetState = stateName;
        return this;
    }
    
    public ConditionalStateBuilder delay(int ms) {
        this.customDelay = ms;
        return this;
    }
    
    public ConditionalStateBuilder lifecycle(IStateLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    public ConditionalStateBuilder when(IGuard guard, String targetState) {
        return when(guard, targetState, null);
    }
    
    public ConditionalStateBuilder when(IGuard guard, String targetState, IAction action) {
        transitions.add(new ConditionalTransitionImpl(guard, targetState, action));
        return this;
    }
    
    public ConditionalStateBuilder defaultTo(String stateName) {
        this.defaultState = stateName;
        return this;
    }
    
    public IConditionalState build() {
        if (transitions.isEmpty()) {
            throw new IllegalStateException("Conditional state must have at least one transition");
        }
        if (defaultState == null || defaultState.trim().isEmpty()) {
            throw new IllegalStateException("Default state is required for conditional state");
        }
        return new ConditionalStateImpl(name, guard, action, nextState, targetState,
                                       customDelay, lifecycle, transitions, defaultState);
    }
}
