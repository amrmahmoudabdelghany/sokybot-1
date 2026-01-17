package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.RetryStateImpl;

/**
 * Builder for retry states.
 */
public class RetryStateBuilder {
    private final String name;
    private IGuard guard;
    private IAction action;
    private String nextState;
    private String targetState;
    private Integer customDelay;
    private IStateLifecycle lifecycle;
    
    private String retryState;
    private int maxRetries;
    private Integer retryDelay;
    private IGuard retryGuard;
    private String exhaustedState;
    
    public RetryStateBuilder(String name) {
        this.name = name;
    }
    
    public RetryStateBuilder guard(IGuard guard) {
        this.guard = guard;
        return this;
    }
    
    public RetryStateBuilder action(IAction action) {
        this.action = action;
        return this;
    }
    
    public RetryStateBuilder nextState(String stateName) {
        this.nextState = stateName;
        return this;
    }
    
    public RetryStateBuilder targetState(String stateName) {
        this.targetState = stateName;
        return this;
    }
    
    public RetryStateBuilder delay(int ms) {
        this.customDelay = ms;
        return this;
    }
    
    public RetryStateBuilder lifecycle(IStateLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    public RetryStateBuilder retry(String stateName) {
        this.retryState = stateName;
        return this;
    }
    
    public RetryStateBuilder maxRetries(int max) {
        this.maxRetries = max;
        return this;
    }
    
    public RetryStateBuilder retryDelay(int ms) {
        this.retryDelay = ms;
        return this;
    }
    
    public RetryStateBuilder whileGuard(IGuard guard) {
        this.retryGuard = guard;
        return this;
    }
    
    public RetryStateBuilder orGoTo(String stateName) {
        this.exhaustedState = stateName;
        return this;
    }
    
    public IRetryState build() {
        if (retryState == null || retryState.trim().isEmpty()) {
            throw new IllegalStateException("Retry state is required");
        }
        if (maxRetries < 0) {
            throw new IllegalStateException("Max retries must be non-negative");
        }
        if (exhaustedState == null || exhaustedState.trim().isEmpty()) {
            throw new IllegalStateException("Exhausted state is required");
        }
        return new RetryStateImpl(name, guard, action, nextState, targetState,
                                 customDelay, lifecycle, retryState, maxRetries,
                                 retryDelay, retryGuard, exhaustedState);
    }
}
