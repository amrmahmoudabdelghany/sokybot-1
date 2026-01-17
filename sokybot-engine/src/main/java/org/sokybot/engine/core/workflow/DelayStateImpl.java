package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

/**
 * Implementation of delay state.
 */
public class DelayStateImpl extends CycleStateImpl implements IDelayState {
    
    private final int delayMs;
    private final int minDelayMs;
    private final IAction delayAction;
    
    public DelayStateImpl(String name, IGuard guard, IAction action,
                         String nextState, String targetState,
                         Integer customDelay, IStateLifecycle lifecycle,
                         int delayMs, int minDelayMs, IAction delayAction) {
        super(name, guard, action, nextState, targetState, customDelay, lifecycle, StateType.DELAY);
        this.delayMs = delayMs;
        this.minDelayMs = minDelayMs;
        this.delayAction = delayAction;
    }
    
    @Override
    public int getDelayMs() {
        return delayMs;
    }
    
    @Override
    public int getMinDelayMs() {
        return minDelayMs;
    }
    
    @Override
    public IAction getDelayAction() {
        return delayAction;
    }
}
