package org.sokybot.engine.core.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.workflow.*;
import org.sokybot.engine.test.util.WorkflowTestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WorkflowRegistryImpl.
 */
@DisplayName("WorkflowRegistryImpl Tests")
class WorkflowRegistryImplTest {
    
    private WorkflowRegistryImpl registry;
    
    @BeforeEach
    void setUp() {
        registry = new WorkflowRegistryImpl();
    }
    
    @Test
    @DisplayName("Should register and unregister orthogonal states")
    void testRegisterUnregisterOrthogonalState() {
        IOrthogonalState state = createMockOrthogonalState("test-state", 100);
        
        int priority = registry.registerOrthogonalState(state);
        
        assertEquals(100, priority);
        assertTrue(registry.getOrderedStates().contains("test-state"));
        assertSame(state, registry.getOrthogonalState("test-state"));
        
        assertTrue(registry.unregisterOrthogonalState("test-state"));
        assertFalse(registry.getOrderedStates().contains("test-state"));
        assertNull(registry.getOrthogonalState("test-state"));
    }
    
    @Test
    @DisplayName("Should register and unregister cycles")
    void testRegisterUnregisterCycle() {
        ICycleDefinition cycle = WorkflowTestBuilders.cycle("test-cycle")
            .priority(200)
            .entryState("STATE1")
            .state("STATE1", WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(), null)
            .build();
        
        int priority = registry.registerCycle(cycle);
        
        assertEquals(200, priority);
        assertTrue(registry.getRegisteredCycles().contains("test-cycle"));
        assertSame(cycle, registry.getCycle("test-cycle"));
        
        assertTrue(registry.unregisterCycle("test-cycle"));
        assertFalse(registry.getRegisteredCycles().contains("test-cycle"));
        assertNull(registry.getCycle("test-cycle"));
    }
    
    @Test
    @DisplayName("Should resolve priority conflicts")
    void testPriorityConflictResolution() {
        IOrthogonalState state1 = createMockOrthogonalState("state1", 100);
        IOrthogonalState state2 = createMockOrthogonalState("state2", 100);
        
        int priority1 = registry.registerOrthogonalState(state1);
        int priority2 = registry.registerOrthogonalState(state2);
        
        assertEquals(100, priority1);
        assertEquals(101, priority2); // Should resolve conflict
    }
    
    @Test
    @DisplayName("Should maintain priority ordering")
    void testPriorityOrdering() {
        IOrthogonalState state1 = createMockOrthogonalState("state1", 300);
        IOrthogonalState state2 = createMockOrthogonalState("state2", 100);
        IOrthogonalState state3 = createMockOrthogonalState("state3", 200);
        
        registry.registerOrthogonalState(state1);
        registry.registerOrthogonalState(state2);
        registry.registerOrthogonalState(state3);
        
        var orderedStates = registry.getOrderedStates();
        assertEquals(3, orderedStates.size());
        assertEquals("state2", orderedStates.get(0)); // Lowest priority first
        assertEquals("state3", orderedStates.get(1));
        assertEquals("state1", orderedStates.get(2));
    }
    
    @Test
    @DisplayName("Should enable and disable cycles")
    void testEnableDisableCycle() {
        // Create CycleDefinitionImpl directly to support enable/disable
        ICycleState state1 = mock(ICycleState.class);
        when(state1.getStateId()).thenReturn(StateId.of("STATE1"));
        
        ICycleDefinition cycle = new CycleDefinitionImpl(
            "test-cycle", 100, "STATE1", 
            WorkflowTestBuilders.guard().returns(true).build(),
            null, 0, null, false, null,
            java.util.List.of(state1));
        
        registry.registerCycle(cycle);
        
        assertTrue(registry.isCycleEnabled("test-cycle"));
        
        registry.setCycleEnabled("test-cycle", false);
        assertFalse(registry.isCycleEnabled("test-cycle"));
        
        registry.setCycleEnabled("test-cycle", true);
        assertTrue(registry.isCycleEnabled("test-cycle"));
    }
    
    @Test
    @DisplayName("Should throw exception when registering duplicate state")
    void testRegisterDuplicateState() {
        IOrthogonalState state = createMockOrthogonalState("test-state", 100);
        
        registry.registerOrthogonalState(state);
        
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerOrthogonalState(state);
        });
    }
    
    @Test
    @DisplayName("Should allow registration while cycle is active")
    void testRegisterWhileCycleActive() {
        registry.setCycleActive(true);
        
        IOrthogonalState state = createMockOrthogonalState("test-state", 100);

        int priority = registry.registerOrthogonalState(state);
        assertEquals(100, priority);
        assertNotNull(registry.getOrthogonalState("test-state"));
    }
    
    @Test
    @DisplayName("Should get ordered workflow components (states and cycles)")
    void testGetOrderedWorkflowComponents() {
        IOrthogonalState state1 = createMockOrthogonalState("state1", 100);
        IOrthogonalState state2 = createMockOrthogonalState("state2", 300);
        
        ICycleDefinition cycle1 = WorkflowTestBuilders.cycle("cycle1")
            .priority(200)
            .entryState("STATE1")
            .state("STATE1", WorkflowTestBuilders.guard().returns(true).build(),
                   WorkflowTestBuilders.action().build(), null)
            .build();
        
        registry.registerOrthogonalState(state1);
        registry.registerCycle(cycle1);
        registry.registerOrthogonalState(state2);
        
        var components = registry.getOrderedWorkflowComponents();
        
        assertEquals(3, components.size());
        assertEquals("state1", components.get(0)); // Priority 100
        assertEquals("cycle1", components.get(1)); // Priority 200
        assertEquals("state2", components.get(2)); // Priority 300
    }
    
    private IOrthogonalState createMockOrthogonalState(String name, int priority) {
        IOrthogonalState state = mock(IOrthogonalState.class);
        when(state.getName()).thenReturn(name);
        when(state.getStateId()).thenReturn(StateId.of(name));
        when(state.getDesiredPriority()).thenReturn(priority);
        when(state.getGuard()).thenReturn(null);
        when(state.getAction()).thenReturn(null);
        when(state.getNextState()).thenReturn(null);
        when(state.getTargetState()).thenReturn(null);
        when(state.getCustomDelay()).thenReturn(null);
        when(state.getLifecycle()).thenReturn(null);
        return state;
    }
}
