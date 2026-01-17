package org.sokybot.engine.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.core.workflow.WorkflowRegistryImpl;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.test.util.MockActuator;
import org.sokybot.engine.test.util.OSGiTestUtils;
import org.sokybot.engine.test.util.WorkflowTestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActuatorRegistry.
 */
@DisplayName("ActuatorRegistry Tests")
class ActuatorRegistryTest {
    
    private ActuatorRegistry registry;
    private WorkflowRegistryImpl workflowRegistry;
    private WorkflowContextImpl workflowContext;
    private EngineCore engineCore;
    private OSGiTestUtils.MockBundleContext mockBundleContext;
    
    @BeforeEach
    public void setUp() {
        workflowRegistry = new WorkflowRegistryImpl();
        workflowContext = mock(WorkflowContextImpl.class);
        when(workflowContext.getGameModel()).thenReturn(mock(org.sokybot.gamemodel.IGameModel.class));
        when(workflowContext.getSettings()).thenReturn(mock(org.sokybot.settings.Settings.class));
        
        engineCore = mock(EngineCore.class);
        when(engineCore.getMachineId()).thenReturn("test-machine");
        when(engineCore.getDispatcher()).thenReturn(mock(org.sokybot.engine.core.dispatcher.DispatcherImpl.class));
        when(engineCore.getSettingsManager()).thenReturn(mock(org.sokybot.settings.ISettingsManager.class));
        
        mockBundleContext = OSGiTestUtils.createMockBundleContext();
        
        registry = new ActuatorRegistry(
            workflowRegistry, workflowContext, engineCore, mockBundleContext);
    }
    
    @Test
    @DisplayName("Should discover and initialize actuators via OSGi")
    void testDiscoverActuatorsViaOSGi() throws BundleException {
        // Create mock actuators
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
        
        // Register actuators as OSGi services
        mockBundleContext.registerMockService(IActuator.class, actuator1, null);
        mockBundleContext.registerMockService(IActuator.class, actuator2, null);
        
        // Initialize actuators
        registry.initializeActuators();
        
        // Verify actuators were initialized
        assertTrue(actuator1.isInitialized());
        assertTrue(actuator2.isInitialized());
        
        // Verify cycles were registered
        assertNotNull(workflowRegistry.getCycle("cycle1"));
        assertNotNull(workflowRegistry.getCycle("cycle2"));
    }
    
    @Test
    @DisplayName("Should handle actuator initialization failure gracefully")
    void testActuatorInitializationFailure() {
        BundleException initException = new BundleException("Initialization failed");
        MockActuator failingActuator = new MockActuator("failing-actuator")
            .withInitializationException(initException);
        
        mockBundleContext.registerMockService(IActuator.class, failingActuator, null);
        
        // Should not throw exception, but log error
        assertDoesNotThrow(() -> {
            registry.initializeActuators();
        });
        
        // Actuator should not be initialized
        assertFalse(failingActuator.isInitialized());
    }
    
    @Test
    @DisplayName("Should register actuator manually")
    void testRegisterActuatorManually() throws BundleException {
        MockActuator actuator = new MockActuator("manual-actuator")
            .withCycle(WorkflowTestBuilders.cycle("manual-cycle")
                .priority(100)
                .entryState("STATE1")
                .state("STATE1",
                       WorkflowTestBuilders.guard().returns(true).build(),
                       WorkflowTestBuilders.action().build(),
                       null)
                .build());
        
        registry.registerActuator(actuator);
        
        assertTrue(actuator.isInitialized());
        assertNotNull(workflowRegistry.getCycle("manual-cycle"));
    }
    
    @Test
    @DisplayName("Should shutdown actuators on shutdown")
    void testShutdownActuators() throws BundleException {
        MockActuator actuator1 = new MockActuator("actuator1");
        MockActuator actuator2 = new MockActuator("actuator2");
        
        mockBundleContext.registerMockService(IActuator.class, actuator1, null);
        mockBundleContext.registerMockService(IActuator.class, actuator2, null);
        
        registry.initializeActuators();
        
        // Shutdown
        registry.shutdownActuators();
        
        // Verify actuators were shut down
        assertTrue(actuator1.isShutdown());
        assertTrue(actuator2.isShutdown());
    }
    
    @Test
    @DisplayName("Should throw exception for null actuator")
    void testRegisterNullActuator() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerActuator(null);
        });
    }
    
    @Test
    @DisplayName("Should handle null BundleContext gracefully")
    void testNullBundleContext() {
        ActuatorRegistry registryWithNullContext = new ActuatorRegistry(
            workflowRegistry, workflowContext, engineCore, null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            registryWithNullContext.initializeActuators();
        });
    }
}
