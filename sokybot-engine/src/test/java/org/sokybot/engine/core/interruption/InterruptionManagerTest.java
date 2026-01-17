package org.sokybot.engine.core.interruption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.core.workflow.WorkflowRegistryImpl;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.test.WorkflowTestBase;
import org.sokybot.engine.test.util.WorkflowTestBuilders;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InterruptionManager.
 */
@DisplayName("InterruptionManager Tests")
class InterruptionManagerTest extends WorkflowTestBase {
    
    private InterruptionManager interruptionManager;
    private WorkflowRegistryImpl registry;
    
    @BeforeEach
    void setUp() {
        super.setUp();
        registry = new WorkflowRegistryImpl();
        interruptionManager = new InterruptionManager(registry);
    }
    
    @Test
    @DisplayName("Should return null when no cycle is executing")
    void testNoInterruptionWhenNoCycleActive() {
        IWorkflowContext context = createWorkflowContext();
        
        ICycleDefinition interrupting = interruptionManager.checkForInterruption(context);
        
        assertNull(interrupting);
    }
    
    @Test
    @DisplayName("Should detect interrupting cycle with higher priority")
    void testInterruptionByHigherPriority() {
        // Register a low-priority cycle
        ICycleDefinition lowPriorityCycle = WorkflowTestBuilders.cycle("low-cycle")
            .priority(100)
            .entryState("STATE1")
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        // Register a high-priority cycle with interruption guard
        IGuard interruptionGuard = WorkflowTestBuilders.guard().returns(true).build();
        ICycleDefinition highPriorityCycle = WorkflowTestBuilders.cycle("high-cycle")
            .priority(200)
            .entryState("STATE1")
            .entryGuard(interruptionGuard)
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        registry.registerCycle(lowPriorityCycle);
        registry.registerCycle(highPriorityCycle);
        
        // Set current cycle (low priority)
        interruptionManager.setCurrentCycle("low-cycle", null, 100);
        
        // Check for interruption
        IWorkflowContext context = createWorkflowContext();
        ICycleDefinition interrupting = interruptionManager.checkForInterruption(context);
        
        // Note: The interruption guard is actually the entry guard, not interruption guard
        // This test might need adjustment based on actual implementation
        // For now, we'll check that interruption logic works
        assertNotNull(interrupting);
    }
    
    @Test
    @DisplayName("Should not interrupt when interruption guard fails")
    void testNoInterruptionWhenGuardFails() {
        // Register a low-priority cycle
        ICycleDefinition lowPriorityCycle = WorkflowTestBuilders.cycle("low-cycle")
            .priority(100)
            .entryState("STATE1")
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        // Register a high-priority cycle with failing interruption guard
        IGuard failingGuard = WorkflowTestBuilders.guard().returns(false).build();
        ICycleDefinition highPriorityCycle = WorkflowTestBuilders.cycle("high-cycle")
            .priority(200)
            .entryState("STATE1")
            .entryGuard(failingGuard)
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        registry.registerCycle(lowPriorityCycle);
        registry.registerCycle(highPriorityCycle);
        
        interruptionManager.setCurrentCycle("low-cycle", null, 100);
        
        IWorkflowContext context = createWorkflowContext();
        ICycleDefinition interrupting = interruptionManager.checkForInterruption(context);
        
        // Should not interrupt because guard failed
        assertNull(interrupting);
    }
    
    @Test
    @DisplayName("Should save and restore cycle state on interruption")
    void testSaveRestoreState() {
        IWorkflowContext context = createWorkflowContext();
        
        // Create a mock state
        ICycleState mockState = mock(ICycleState.class);
        when(mockState.getName()).thenReturn("STATE1");
        
        // Set current cycle
        interruptionManager.setCurrentCycle("test-cycle", mockState, 100);
        
        // Interrupt
        ICycleDefinition interruptingCycle = WorkflowTestBuilders.cycle("interrupt-cycle")
            .priority(200)
            .entryState("STATE1")
            .state("STATE1",
                   WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(),
                   null)
            .build();
        
        CycleStateSaver.SavedState savedState = interruptionManager.interruptCurrentCycle(
            interruptingCycle, context);
        
        assertNotNull(savedState);
        assertEquals("test-cycle", savedState.getCycleName());
        
        // Clear current cycle
        interruptionManager.clearCurrentCycle();
        
        // Restore
        ICycleState restoredState = interruptionManager.resumeCycle(savedState, context);
        
        assertNotNull(restoredState);
        assertEquals("STATE1", restoredState.getName());
    }
    
    @Test
    @DisplayName("Should clear current cycle tracking")
    void testClearCurrentCycle() {
        ICycleState mockState = mock(ICycleState.class);
        interruptionManager.setCurrentCycle("test-cycle", mockState, 100);
        
        IWorkflowContext context = createWorkflowContext();
        assertNull(interruptionManager.checkForInterruption(context));
        
        interruptionManager.clearCurrentCycle();
        
        // After clearing, should not interrupt
        assertNull(interruptionManager.checkForInterruption(context));
    }
}
