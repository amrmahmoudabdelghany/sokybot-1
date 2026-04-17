package org.sokybot.engine.core.workflow.builder;

import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.CycleDefinitionImpl;

import java.util.*;
import java.util.function.Function;

/**
 * Fluent builder for cycle definitions.
 * Provides a convenient API for creating cycles.
 */
public class CycleDefinitionBuilder {
    private String name;
    private int priority;
    private String entryState;
    private IGuard entryGuard;
    private IGuard interruptionGuard;
    private int interruptionPriority;
    private IAction interruptionAction;
    private boolean interruptible = true;
    private ICycleLifecycle lifecycle;
    
    private final List<ICycleState> states = new ArrayList<>();
    private final Map<String, Object> stateBuilders = new HashMap<>();
    
    /**
     * Sets the cycle name.
     */
    public CycleDefinitionBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CycleDefinitionBuilder cycle(CycleId cycleId) {
        this.name = cycleId != null ? cycleId.asString() : null;
        return this;
    }
    
    /**
     * Sets the cycle priority (lower = earlier in overall cycle).
     */
    public CycleDefinitionBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }
    
    /**
     * Sets the entry state name (first state in cycle).
     */
    public CycleDefinitionBuilder entryState(String stateName) {
        this.entryState = stateName;
        return this;
    }

    public CycleDefinitionBuilder entryState(StateId stateId) {
        this.entryState = stateId != null ? stateId.asString() : null;
        return this;
    }
    
    /**
     * Sets the entry guard (determines if cycle should be entered).
     */
    public CycleDefinitionBuilder entryGuard(IGuard guard) {
        this.entryGuard = guard;
        return this;
    }
    
    /**
     * Sets the interruption guard (allows this cycle to interrupt others).
     */
    public CycleDefinitionBuilder interruptionGuard(IGuard guard) {
        this.interruptionGuard = guard;
        return this;
    }
    
    /**
     * Sets the interruption priority (higher = can interrupt lower priority cycles).
     */
    public CycleDefinitionBuilder interruptionPriority(int priority) {
        this.interruptionPriority = priority;
        return this;
    }
    
    /**
     * Sets the interruption action (executed when interrupting).
     */
    public CycleDefinitionBuilder interruptionAction(IAction action) {
        this.interruptionAction = action;
        return this;
    }
    
    /**
     * Sets whether this cycle can be interrupted.
     */
    public CycleDefinitionBuilder interruptible(boolean interruptible) {
        this.interruptible = interruptible;
        return this;
    }
    
    /**
     * Sets the cycle lifecycle hooks.
     */
    public CycleDefinitionBuilder lifecycle(ICycleLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
    
    /**
     * Creates a standard state.
     */
    public CycleDefinitionBuilder state(String name, Function<StateBuilder, StateBuilder> config) {
        StateBuilder builder = new StateBuilder(name);
        config.apply(builder);
        states.add(builder.build());
        return this;
    }
    
    /**
     * Creates a loop state.
     */
    public CycleDefinitionBuilder loopState(String name, Function<LoopStateBuilder, LoopStateBuilder> config) {
        LoopStateBuilder builder = new LoopStateBuilder(name);
        config.apply(builder);
        states.add(builder.build());
        return this;
    }
    
    /**
     * Creates a delay state.
     */
    public CycleDefinitionBuilder delayState(String name, Function<DelayStateBuilder, DelayStateBuilder> config) {
        DelayStateBuilder builder = new DelayStateBuilder(name);
        config.apply(builder);
        states.add(builder.build());
        return this;
    }
    
    /**
     * Creates an exit state.
     */
    public CycleDefinitionBuilder exitState(String name, Function<ExitStateBuilder, ExitStateBuilder> config) {
        ExitStateBuilder builder = new ExitStateBuilder(name);
        config.apply(builder);
        states.add(builder.build());
        return this;
    }
    
    /**
     * Creates a conditional state.
     */
    public CycleDefinitionBuilder conditionalState(String name, Function<ConditionalStateBuilder, ConditionalStateBuilder> config) {
        ConditionalStateBuilder builder = new ConditionalStateBuilder(name);
        config.apply(builder);
        states.add(builder.build());
        return this;
    }
    
    /**
     * Creates a guard-only state.
     */
    public CycleDefinitionBuilder guardState(String name, Function<GuardStateBuilder, GuardStateBuilder> config) {
        GuardStateBuilder builder = new GuardStateBuilder(name);
        config.apply(builder);
        states.add(builder.build());
        return this;
    }
    
    /**
     * Creates a retry state.
     */
    public CycleDefinitionBuilder retryState(String name, Function<RetryStateBuilder, RetryStateBuilder> config) {
        RetryStateBuilder builder = new RetryStateBuilder(name);
        config.apply(builder);
        states.add(builder.build());
        return this;
    }
    
    /**
     * Builds the cycle definition.
     */
    public ICycleDefinition build() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalStateException("Cycle name is required");
        }
        if (entryState == null || entryState.trim().isEmpty()) {
            throw new IllegalStateException("Entry state is required");
        }
        if (states.isEmpty()) {
            throw new IllegalStateException("Cycle must have at least one state");
        }
        
        // Use interruption priority if set, otherwise use desired priority
        int actualInterruptionPriority = interruptionPriority != 0 ? interruptionPriority : priority;
        
        return new CycleDefinitionImpl(name, priority, entryState,
                                      entryGuard, interruptionGuard,
                                      actualInterruptionPriority, interruptionAction,
                                      interruptible, lifecycle, states);
    }
}
