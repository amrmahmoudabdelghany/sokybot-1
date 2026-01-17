package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

/**
 * Implementation of guard-only state.
 */
public class GuardOnlyStateImpl extends CycleStateImpl implements IGuardOnlyState {
    
    private final String successState;
    private final String failureState;
    
    public GuardOnlyStateImpl(String name, IGuard guard,
                             String successState, String failureState,
                             Integer customDelay, IStateLifecycle lifecycle) {
        super(name, guard, null, successState, failureState, customDelay, lifecycle, StateType.GUARD_ONLY);
        if (guard == null) {
            throw new IllegalArgumentException("Guard cannot be null for guard-only state");
        }
        if (successState == null || successState.trim().isEmpty()) {
            throw new IllegalArgumentException("Success state cannot be null or empty");
        }
        if (failureState == null || failureState.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure state cannot be null or empty");
        }
        this.successState = successState;
        this.failureState = failureState;
    }
    
    @Override
    public String getSuccessState() {
        return successState;
    }
    
    @Override
    public String getFailureState() {
        return failureState;
    }
}
