package org.sokybot.engine.test.util;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sokybot.engine.api.workflow.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builders for creating test workflow components (guards, actions, cycles, states).
 */
public class WorkflowTestBuilders {
    
    /**
     * Creates a mock guard that returns the specified value.
     */
    public static IGuard createMockGuard(boolean shouldPass) {
        IGuard guard = Mockito.mock(IGuard.class);
        Mockito.when(guard.evaluate(Mockito.any(IWorkflowContext.class))).thenReturn(shouldPass);
        return guard;
    }
    
    /**
     * Creates a configurable mock guard builder.
     */
    public static MockGuardBuilder guard() {
        return new MockGuardBuilder();
    }
    
    /**
     * Builder for creating mock guards with configurable behavior.
     */
    public static class MockGuardBuilder {
        private boolean returnValue = true;
        private WorkflowException exceptionToThrow = null;
        private final List<IWorkflowContext> evaluatedContexts = new ArrayList<>();
        private int evaluationCount = 0;
        
        /**
         * Sets the return value for the guard.
         */
        public MockGuardBuilder returns(boolean value) {
            this.returnValue = value;
            return this;
        }
        
        /**
         * Sets an exception to throw when evaluated.
         */
        public MockGuardBuilder throwsException(WorkflowException exception) {
            this.exceptionToThrow = exception;
            return this;
        }
        
        /**
         * Builds the mock guard.
         */
        public IGuard build() {
            IGuard guard = Mockito.mock(IGuard.class);
            Mockito.when(guard.evaluate(Mockito.any(IWorkflowContext.class))).thenAnswer(invocation -> {
                evaluationCount++;
                IWorkflowContext context = invocation.getArgument(0);
                evaluatedContexts.add(context);
                if (exceptionToThrow != null) {
                    throw exceptionToThrow;
                }
                return returnValue;
            });
            return guard;
        }
        
        /**
         * Gets the number of times the guard was evaluated.
         */
        public int getEvaluationCount() {
            return evaluationCount;
        }
        
        /**
         * Gets all contexts the guard was evaluated with.
         */
        public List<IWorkflowContext> getEvaluatedContexts() {
            return new ArrayList<>(evaluatedContexts);
        }
    }
    
    /**
     * Creates a mock action that does nothing.
     */
    public static IAction createMockAction() {
        return Mockito.mock(IAction.class);
    }
    
    /**
     * Creates a configurable mock action builder.
     */
    public static MockActionBuilder action() {
        return new MockActionBuilder();
    }
    
    /**
     * Builder for creating mock actions with configurable behavior.
     */
    public static class MockActionBuilder {
        private WorkflowException exceptionToThrow = null;
        private Runnable onExecute;
        private final List<IWorkflowContext> executedContexts = new ArrayList<>();
        private int executionCount = 0;
        private final AtomicBoolean executed = new AtomicBoolean(false);
        
        /**
         * Sets an exception to throw when executed.
         */
        public MockActionBuilder throwsException(WorkflowException exception) {
            this.exceptionToThrow = exception;
            return this;
        }
        
        /**
         * Sets a callback to run when the action executes.
         */
        public MockActionBuilder withCallback(Runnable callback) {
            this.onExecute = callback;
            return this;
        }
        
        /**
         * Builds the mock action.
         */
        public IAction build() {
            IAction action = Mockito.mock(IAction.class);
            Mockito.doAnswer(invocation -> {
                executionCount++;
                executed.set(true);
                IWorkflowContext context = invocation.getArgument(0);
                executedContexts.add(context);
                if (onExecute != null) {
                    onExecute.run();
                }
                if (exceptionToThrow != null) {
                    throw exceptionToThrow;
                }
                return null;
            }).when(action).execute(Mockito.any(IWorkflowContext.class));
            return action;
        }
        
        /**
         * Gets the number of times the action was executed.
         */
        public int getExecutionCount() {
            return executionCount;
        }
        
        /**
         * Gets all contexts the action was executed with.
         */
        public List<IWorkflowContext> getExecutedContexts() {
            return new ArrayList<>(executedContexts);
        }
        
        /**
         * Checks if the action was executed at least once.
         */
        public boolean wasExecuted() {
            return executed.get();
        }
    }
    
    /**
     * Creates a simple test state.
     */
    public static IWorkflowState createSimpleState(String name, IGuard guard, IAction action, String nextState) {
        return new SimpleTestState(name, guard, action, nextState);
    }
    
    /**
     * Simple test state implementation.
     */
    private static class SimpleTestState implements IWorkflowState {
        private final String name;
        private final IGuard guard;
        private final IAction action;
        private final String nextState;
        private final String targetState;
        
        SimpleTestState(String name, IGuard guard, IAction action, String nextState) {
            this(name, guard, action, nextState, null);
        }
        
        SimpleTestState(String name, IGuard guard, IAction action, String nextState, String targetState) {
            this.name = name;
            this.guard = guard;
            this.action = action;
            this.nextState = nextState;
            this.targetState = targetState;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public IGuard getGuard() {
            return guard;
        }
        
        @Override
        public IAction getAction() {
            return action;
        }
        
        @Override
        public String getNextState() {
            return nextState;
        }
        
        @Override
        public String getTargetState() {
            return targetState;
        }
        
        @Override
        public Integer getCustomDelay() {
            return null;
        }
        
        @Override
        public IStateLifecycle getLifecycle() {
            return null;
        }
    }
    
    /**
     * Creates a simple test cycle using the builder pattern.
     */
    public static TestCycleBuilder cycle(String name) {
        return new TestCycleBuilder(name);
    }
    
    /**
     * Builder for creating test cycles.
     */
    public static class TestCycleBuilder {
        private final String name;
        private int priority = 100;
        private String entryState;
        private IGuard entryGuard;
        private final List<IWorkflowState> states = new ArrayList<>();
        
        TestCycleBuilder(String name) {
            this.name = name;
        }
        
        /**
         * Sets the cycle priority.
         */
        public TestCycleBuilder priority(int priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Sets the entry state.
         */
        public TestCycleBuilder entryState(String stateName) {
            this.entryState = stateName;
            return this;
        }
        
        /**
         * Sets the entry guard.
         */
        public TestCycleBuilder entryGuard(IGuard guard) {
            this.entryGuard = guard;
            return this;
        }
        
        /**
         * Adds a state to the cycle.
         */
        public TestCycleBuilder state(IWorkflowState state) {
            this.states.add(state);
            return this;
        }
        
        /**
         * Adds a simple state with guard and action.
         */
        public TestCycleBuilder state(String name, IGuard guard, IAction action, String nextState) {
            this.states.add(createSimpleState(name, guard, action, nextState));
            return this;
        }
        
        /**
         * Builds the cycle definition.
         */
        public ICycleDefinition build() {
            if (entryState == null && !states.isEmpty()) {
                entryState = states.get(0).getName();
            }
            
            return new SimpleTestCycle(name, priority, entryState, entryGuard, states);
        }
    }
    
    /**
     * Simple test cycle implementation.
     */
    private static class SimpleTestCycle implements ICycleDefinition {
        private final String name;
        private final int priority;
        private final String entryState;
        private final IGuard entryGuard;
        private final List<ICycleState> states;
        private final Map<String, ICycleState> statesMap;
        
        SimpleTestCycle(String name, int priority, String entryState, IGuard entryGuard, List<IWorkflowState> states) {
            this.name = name;
            this.priority = priority;
            this.entryState = entryState;
            this.entryGuard = entryGuard;
            // Convert IWorkflowState to ICycleState if needed
            this.statesMap = new HashMap<>();
            List<ICycleState> cycleStates = new ArrayList<>();
            for (IWorkflowState state : states) {
                ICycleState cycleState = state instanceof ICycleState 
                    ? (ICycleState) state 
                    : new WrapperCycleState(state);
                cycleStates.add(cycleState);
                statesMap.put(state.getName(), cycleState);
            }
            this.states = cycleStates;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public int getDesiredPriority() {
            return priority;
        }
        
        @Override
        public String getEntryStateName() {
            return entryState;
        }
        
        @Override
        public IGuard getEntryGuard() {
            return entryGuard;
        }
        
        @Override
        public List<ICycleState> getStates() {
            return new ArrayList<>(states);
        }
        
        @Override
        public IGuard getInterruptionGuard() {
            return null;
        }
        
        @Override
        public int getInterruptionPriority() {
            return 0;
        }
        
        @Override
        public IAction getInterruptionAction() {
            return null;
        }
        
        @Override
        public boolean isInterruptible() {
            return false;
        }
        
        @Override
        public ICycleState getState(String stateName) {
            return statesMap.get(stateName);
        }
        
        @Override
        public ICycleLifecycle getLifecycle() {
            return null;
        }
    }
    
    /**
     * Wrapper to convert IWorkflowState to ICycleState.
     */
    private static class WrapperCycleState implements ICycleState {
        private final IWorkflowState delegate;
        
        WrapperCycleState(IWorkflowState delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public String getName() { return delegate.getName(); }
        @Override
        public IGuard getGuard() { return delegate.getGuard(); }
        @Override
        public IAction getAction() { return delegate.getAction(); }
        @Override
        public String getNextState() { return delegate.getNextState(); }
        @Override
        public String getTargetState() { return delegate.getTargetState(); }
        @Override
        public Integer getCustomDelay() { return delegate.getCustomDelay(); }
        @Override
        public IStateLifecycle getLifecycle() { return delegate.getLifecycle(); }
        @Override
        public StateType getStateType() { return StateType.STANDARD; }
        @Override
        @SuppressWarnings("unchecked")
        public <T extends IWorkflowState> T getAs(Class<T> type) {
            return type.isInstance(this) ? (T) this : null;
        }
    }
}
