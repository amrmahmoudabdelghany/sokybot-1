package org.sokybot.engine.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.core.EngineCore;
import org.sokybot.engine.core.WorkflowRegistryImpl;
import org.sokybot.engine.test.EngineTestBase;
import org.sokybot.engine.test.util.MockActuator;
import org.sokybot.engine.test.util.WorkflowTestBuilders;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for actuator lifecycle and registration.
 */
@DisplayName("Actuator Integration Tests")
class ActuatorIntegrationTest extends EngineTestBase {
    
    private EngineCore engine;
    
    @BeforeEach
    void setUp() {
        super.setUp();
    }
    
    @Test
    @DisplayName("Should discover and initialize actuators via OSGi")
    void testActuatorDiscoveryAndInitialization() throws BundleException {
        // Register actuators as OSGi services
        MockActuator actuator1 = new MockActuator("actuator1")
            .withCycle(WorkflowTestBuilders.cycle("cycle1")
                .priority(100)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        MockActuator actuator2 = new MockActuator("actuator2")
            .withCycle(WorkflowTestBuilders.cycle("cycle2")
                .priority(200)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        mockBundleContext.registerService(IActuator.class, actuator1, null);
        mockBundleContext.registerService(IActuator.class, actuator2, null);
        
        // Create and start engine
        engine = createTestEngine();
        engine.start();
        
        // Verify actuators were initialized
        assertTrue(actuator1.isInitialized());
        assertTrue(actuator2.isInitialized());
        
        // Verify cycles were registered
        assertNotNull(engine.getWorkflowRegistry().getCycle("cycle1"));
        assertNotNull(engine.getWorkflowRegistry().getCycle("cycle2"));
        
        engine.stop();
    }
    
    @Test
    @DisplayName("Should handle actuator initialization failure gracefully")
    void testActuatorInitializationFailure() {
        BundleException initException = new BundleException("Initialization failed");
        MockActuator failingActuator = new MockActuator("failing-actuator")
            .withInitializationException(initException);
        
        MockActuator workingActuator = new MockActuator("working-actuator")
            .withCycle(WorkflowTestBuilders.cycle("working-cycle")
                .priority(100)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        mockBundleContext.registerService(IActuator.class, failingActuator, null);
        mockBundleContext.registerService(IActuator.class, workingActuator, null);
        
        // Engine should still start even if one actuator fails
        engine = createTestEngine();
        
        // Start should not throw exception (error is logged)
        assertDoesNotThrow(() -> {
            engine.start();
        });
        
        // Working actuator should still be initialized
        assertTrue(workingActuator.isInitialized());
        
        // Failing actuator should not be initialized
        assertFalse(failingActuator.isInitialized());
        
        engine.stop();
    }
    
    @Test
    @DisplayName("Should shutdown all actuators on engine stop")
    void testActuatorShutdown() throws BundleException {
        MockActuator actuator1 = new MockActuator("actuator1");
        MockActuator actuator2 = new MockActuator("actuator2");
        
        mockBundleContext.registerService(IActuator.class, actuator1, null);
        mockBundleContext.registerService(IActuator.class, actuator2, null);
        
        engine = createTestEngine();
        engine.start();
        
        assertTrue(actuator1.isInitialized());
        assertTrue(actuator2.isInitialized());
        
        // Stop engine
        engine.stop();
        
        // Verify actuators were shut down
        assertTrue(actuator1.isShutdown());
        assertTrue(actuator2.isShutdown());
    }
    
    @Test
    @DisplayName("Should register multiple actuators with different priorities")
    void testMultipleActuatorsWithPriorities() throws BundleException {
        MockActuator actuator1 = new MockActuator("actuator1")
            .withCycle(WorkflowTestBuilders.cycle("cycle1")
                .priority(100)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        MockActuator actuator2 = new MockActuator("actuator2")
            .withCycle(WorkflowTestBuilders.cycle("cycle2")
                .priority(200)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        MockActuator actuator3 = new MockActuator("actuator3")
            .withCycle(WorkflowTestBuilders.cycle("cycle3")
                .priority(50)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        mockBundleContext.registerService(IActuator.class, actuator1, null);
        mockBundleContext.registerService(IActuator.class, actuator2, null);
        mockBundleContext.registerService(IActuator.class, actuator3, null);
        
        engine = createTestEngine();
        engine.start();
        
        // Verify all actuators were initialized
        assertTrue(actuator1.isInitialized());
        assertTrue(actuator2.isInitialized());
        assertTrue(actuator3.isInitialized());
        
        // Verify cycles are ordered by priority
        var orderedComponents = engine.getWorkflowRegistry().getOrderedWorkflowComponents();
        assertTrue(orderedComponents.indexOf("cycle3") < orderedComponents.indexOf("cycle1"));
        assertTrue(orderedComponents.indexOf("cycle1") < orderedComponents.indexOf("cycle2"));
        
        engine.stop();
    }
    
    @Test
    @DisplayName("Should handle actuator recovery after initialization failure")
    void testActuatorRecovery() {
        // This test demonstrates that one actuator failure doesn't prevent others from working
        BundleException initException = new BundleException("Initialization failed");
        MockActuator failingActuator = new MockActuator("failing-actuator")
            .withInitializationException(initException);
        
        MockActuator workingActuator = new MockActuator("working-actuator")
            .withCycle(WorkflowTestBuilders.cycle("recovery-cycle")
                .priority(100)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        mockBundleContext.registerService(IActuator.class, failingActuator, null);
        mockBundleContext.registerService(IActuator.class, workingActuator, null);
        
        engine = createTestEngine();
        
        // Engine should start successfully
        assertDoesNotThrow(() -> {
            engine.start();
        });
        
        // Working actuator's cycle should be available
        assertNotNull(engine.getWorkflowRegistry().getCycle("recovery-cycle"));
        
        engine.stop();
    }
}
