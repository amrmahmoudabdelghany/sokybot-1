package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

/**
 * Implementation of conditional transition.
 */
public class ConditionalTransitionImpl implements IConditionalTransition {
    
    private final IGuard guard;
    private final String targetState;
    private final IAction action;
    
    public ConditionalTransitionImpl(IGuard guard, String targetState, IAction action) {
        if (guard == null) {
            throw new IllegalArgumentException("Guard cannot be null");
        }
        if (targetState == null || targetState.trim().isEmpty()) {
            throw new IllegalArgumentException("Target state cannot be null or empty");
        }
        this.guard = guard;
        this.targetState = targetState;
        this.action = action;
    }
    
    @Override
    public IGuard getGuard() {
        return guard;
    }
    
    @Override
    public String getTargetState() {
        return targetState;
    }
    
    @Override
    public IAction getAction() {
        return action;
    }
}
