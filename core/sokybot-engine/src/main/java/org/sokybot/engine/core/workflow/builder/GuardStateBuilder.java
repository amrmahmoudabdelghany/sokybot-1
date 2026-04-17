package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.GuardOnlyStateImpl;

/**
 * Builder for guard-only states.
 */
public class GuardStateBuilder {
    private final String name;
    private IGuard guard;
    private Integer customDelay;
    private IStateLifecycle lifecycle;
    
    private String successState;
    private String failureState;
    
    public GuardStateBuilder(String name) {
        this.name = name;
    }
    
    public GuardStateBuilder guard(IGuard guard) {
        this.guard = guard;
        return this;
    }
    
    public GuardStateBuilder onSuccess(String stateName) {
        this.successState = stateName;
        return this;
    }
    public GuardStateBuilder onSuccess(StateId stateId) {
        this.successState = stateId != null ? stateId.asString() : null;
        return this;
    }
    
    public GuardStateBuilder onFailure(String stateName) {
        this.failureState = stateName;
        return this;
    }
    public GuardStateBuilder onFailure(StateId stateId) {
        this.failureState = stateId != null ? stateId.asString() : null;
        return this;
    }
    
    public GuardStateBuilder delay(int ms) {
        this.customDelay = ms;
        return this;
    }
    
    public GuardStateBuilder lifecycle(IStateLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    public IGuardOnlyState build() {
        if (guard == null) {
            throw new IllegalStateException("Guard is required for guard-only state");
        }
        if (successState == null || successState.trim().isEmpty()) {
            throw new IllegalStateException("Success state is required");
        }
        if (failureState == null || failureState.trim().isEmpty()) {
            throw new IllegalStateException("Failure state is required");
        }
        return new GuardOnlyStateImpl(name, guard, successState, failureState,
                                     customDelay, lifecycle);
    }
}
