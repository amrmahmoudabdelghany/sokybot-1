package org.sokybot.engine.core.execution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.interruption.InterruptionManager;
import org.sokybot.engine.core.queue.ActionQueueProcessorImpl;
import org.sokybot.engine.core.queue.ActionQueueImpl;
import org.sokybot.engine.core.workflow.WorkflowRegistryImpl;
import org.sokybot.engine.test.WorkflowTestBase;
import org.sokybot.engine.test.util.WorkflowTestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CycleExecutor.
 */
@DisplayName("CycleExecutor Tests")
class CycleExecutorTest extends WorkflowTestBase {
    
    private CycleExecutor executor;
    private InterruptionManager interruptionManager;
    private ActionQueueProcessorImpl queueProcessor;
    private WorkflowRegistryImpl registry;
    
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        registry = new WorkflowRegistryImpl();
        interruptionManager = new InterruptionManager(registry);
        queueProcessor = new ActionQueueProcessorImpl(new ActionQueueImpl());
        executor = new CycleExecutor(interruptionManager, queueProcessor, createWorkflowContext());
    }
    
    @Test
    @DisplayName("Should execute simple cycle with guard passing")
    void testExecuteCycleGuardPassing() {
        WorkflowTestBuilders.MockActionBuilder actionBuilder = WorkflowTestBuilders.action();
        
        ICycleDefinition cycle = WorkflowTestBuilders.cycle("test-cycle")
            .priority(100)
            .entryState("STATE1")
            .state("STATE1", 
                   WorkflowTestBuilders.guard().returns(true).build(),
                   actionBuilder.build(),
                   "STATE2")
            .state("STATE2",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null) // Exit
            .build();
        
        boolean completed = executor.executeCycle(cycle);
        
        assertTrue(completed);
        assertTrue(actionBuilder.wasExecuted());
    }
    
    @Test
    @DisplayName("Should skip action when guard fails")
    void testGuardFailing() {
        WorkflowTestBuilders.MockActionBuilder actionBuilder = WorkflowTestBuilders.action();
        IGuard failingGuard = WorkflowTestBuilders.guard().returns(false).build();
        
        ICycleDefinition cycle = WorkflowTestBuilders.cycle("test-cycle")
            .priority(100)
            .entryState("STATE1")
            .state("STATE1", failingGuard, actionBuilder.build(), "STATE2")
            .state("STATE2",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        boolean completed = executor.executeCycle(cycle);
        
        assertTrue(completed);
        assertFalse(actionBuilder.wasExecuted()); // Action should not execute
    }
    
    @Test
    @DisplayName("Should handle guard exceptions gracefully")
    void testGuardException() {
        WorkflowException exception = new WorkflowException("Guard error");
        IGuard throwingGuard = WorkflowTestBuilders.guard()
            .throwsException(exception)
            .build();
        
        ICycleDefinition cycle = WorkflowTestBuilders.cycle("test-cycle")
            .priority(100)
            .entryState("STATE1")
            .state("STATE1", throwingGuard, WorkflowTestBuilders.action().build(), null)
            .build();
        
        // Should not throw exception, but return false
        boolean completed = executor.executeCycle(cycle);
        assertFalse(completed);
    }
    
    @Test
    @DisplayName("Should handle action exceptions gracefully")
    void testActionException() {
        WorkflowException exception = new WorkflowException("Action error");
        IAction throwingAction = WorkflowTestBuilders.action()
            .throwsException(exception)
            .build();
        
        ICycleDefinition cycle = WorkflowTestBuilders.cycle("test-cycle")
            .priority(100)
            .entryState("STATE1")
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   throwingAction,
                   null)
            .build();
        
        // Should not throw exception, cycle should complete
        boolean completed = executor.executeCycle(cycle);
        assertTrue(completed);
    }
    
    @Test
    @DisplayName("Should respect entry guard")
    void testEntryGuard() {
        IGuard entryGuard = WorkflowTestBuilders.guard().returns(false).build();
        
        ICycleDefinition cycle = WorkflowTestBuilders.cycle("test-cycle")
            .priority(100)
            .entryState("STATE1")
            .entryGuard(entryGuard)
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        boolean completed = executor.executeCycle(cycle);
        assertFalse(completed); // Entry guard failed
    }
    
    @Test
    @DisplayName("Should return false for disabled cycle")
    void testDisabledCycle() {
        ICycleDefinition cycle = WorkflowTestBuilders.cycle("test-cycle")
            .priority(100)
            .entryState("STATE1")
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        registry.registerCycle(cycle);
        registry.setCycleEnabled("test-cycle", false);
        
        ICycleDefinition disabledCycle = registry.getCycle("test-cycle");
        boolean completed = executor.executeCycle(disabledCycle);
        
        assertFalse(completed);
    }
}
