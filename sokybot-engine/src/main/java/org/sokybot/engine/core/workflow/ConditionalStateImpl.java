package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.workflow.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of conditional state.
 */
public class ConditionalStateImpl extends CycleStateImpl implements IConditionalState {
    
    private final List<IConditionalTransition> transitions;
    private final String defaultState;
    
    public ConditionalStateImpl(String name, IGuard guard, IAction action,
                               String nextState, String targetState,
                               Integer customDelay, IStateLifecycle lifecycle,
                               List<IConditionalTransition> transitions, String defaultState) {
        super(name, guard, action, nextState, targetState, customDelay, lifecycle, StateType.CONDITIONAL);
        this.transitions = transitions != null ? new ArrayList<>(transitions) : Collections.emptyList();
        this.defaultState = defaultState;
    }
    
    @Override
    public List<IConditionalTransition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }
    
    @Override
    public String getDefaultState() {
        return defaultState;
    }
}
