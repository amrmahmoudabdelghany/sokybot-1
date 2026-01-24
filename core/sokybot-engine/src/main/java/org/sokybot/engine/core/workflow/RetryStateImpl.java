package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

/**
 * Implementation of retry state.
 */
public class RetryStateImpl extends CycleStateImpl implements IRetryState {
    
    private final String retryState;
    private final int maxRetries;
    private final Integer retryDelay;
    private final IGuard retryGuard;
    private final String exhaustedState;
    
    public RetryStateImpl(String name, IGuard guard, IAction action,
                         String nextState, String targetState,
                         Integer customDelay, IStateLifecycle lifecycle,
                         String retryState, int maxRetries,
                         Integer retryDelay, IGuard retryGuard,
                         String exhaustedState) {
        super(name, guard, action, nextState, targetState, customDelay, lifecycle, StateType.RETRY);
        if (retryState == null || retryState.trim().isEmpty()) {
            throw new IllegalArgumentException("Retry state cannot be null or empty");
        }
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries must be non-negative");
        }
        if (exhaustedState == null || exhaustedState.trim().isEmpty()) {
            throw new IllegalArgumentException("Exhausted state cannot be null or empty");
        }
        this.retryState = retryState;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.retryGuard = retryGuard;
        this.exhaustedState = exhaustedState;
    }
    
    @Override
    public String getRetryState() {
        return retryState;
    }
    
    @Override
    public int getMaxRetries() {
        return maxRetries;
    }
    
    @Override
    public Integer getRetryDelay() {
        return retryDelay;
    }
    
    @Override
    public IGuard getRetryGuard() {
        return retryGuard;
    }
    
    @Override
    public String getExhaustedState() {
        return exhaustedState;
    }
}
