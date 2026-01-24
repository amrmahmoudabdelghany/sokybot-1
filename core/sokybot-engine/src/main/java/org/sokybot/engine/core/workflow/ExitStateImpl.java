package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

/**
 * Implementation of exit state.
 */
public class ExitStateImpl extends CycleStateImpl implements IExitState {
    
    private final IGuard exitGuard;
    private final IAction exitAction;
    private final String continueState;
    
    public ExitStateImpl(String name, IGuard guard, IAction action,
                        String nextState, String targetState,
                        Integer customDelay, IStateLifecycle lifecycle,
                        IGuard exitGuard, IAction exitAction, String continueState) {
        super(name, guard, action, nextState, targetState, customDelay, lifecycle, StateType.EXIT);
        this.exitGuard = exitGuard;
        this.exitAction = exitAction;
        this.continueState = continueState;
    }
    
    @Override
    public IGuard getExitGuard() {
        return exitGuard;
    }
    
    @Override
    public IAction getExitAction() {
        return exitAction;
    }
    
    @Override
    public String getContinueState() {
        return continueState;
    }
}
