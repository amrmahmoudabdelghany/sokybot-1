package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

/**
 * Implementation of loop state.
 */
public class LoopStateImpl extends CycleStateImpl implements ILoopState {
    
    private final String loopBackState;
    private final IGuard loopGuard;
    private final Integer maxIterations;
    private final Integer loopDelay;
    private final String counterKey;
    private final IAction loopExitAction;
    
    public LoopStateImpl(String name, IGuard guard, IAction action,
                        String nextState, String targetState,
                        Integer customDelay, IStateLifecycle lifecycle,
                        String loopBackState, IGuard loopGuard,
                        Integer maxIterations, Integer loopDelay,
                        String counterKey, IAction loopExitAction) {
        super(name, guard, action, nextState, targetState, customDelay, lifecycle, StateType.LOOP);
        this.loopBackState = loopBackState;
        this.loopGuard = loopGuard;
        this.maxIterations = maxIterations;
        this.loopDelay = loopDelay;
        this.counterKey = counterKey;
        this.loopExitAction = loopExitAction;
    }
    
    @Override
    public String getLoopBackState() {
        return loopBackState;
    }
    
    @Override
    public IGuard getLoopGuard() {
        return loopGuard;
    }
    
    @Override
    public Integer getMaxIterations() {
        return maxIterations;
    }
    
    @Override
    public Integer getLoopDelay() {
        return loopDelay;
    }
    
    @Override
    public String getCounterKey() {
        return counterKey;
    }
    
    @Override
    public IAction getLoopExitAction() {
        return loopExitAction;
    }
}
