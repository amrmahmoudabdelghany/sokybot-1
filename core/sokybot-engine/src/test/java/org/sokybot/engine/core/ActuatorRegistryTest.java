package org.sokybot.engine.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.osgi.framework.ServiceReference;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.core.workflow.WorkflowRegistryImpl;
import org.sokybot.engine.test.util.MockActuator;
import org.sokybot.engine.test.util.OSGiTestUtils;
import org.sokybot.engine.test.util.WorkflowTestBuilders;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.engine.IEngine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ActuatorRegistry.
 */
@DisplayName("ActuatorRegistry Tests")
class ActuatorRegistryTest {

        private ActuatorRegistry registry;
        private WorkflowRegistryImpl workflowRegistry;
        private IWorkflowContext workflowContext;
        private IEngine engineCore;
        private OSGiTestUtils.MockBundleContext mockBundleContext;

        @BeforeEach
        public void setUp() {
                workflowRegistry = new WorkflowRegistryImpl();
                workflowContext = mock(IWorkflowContext.class);
                when(workflowContext.getGameModel()).thenReturn(mock(org.sokybot.gamemodel.IGameModel.class));
                when(workflowContext.getGroupName()).thenReturn("test-group");
                when(workflowContext.getMachineName()).thenReturn("test-machine");

                engineCore = mock(IEngine.class);
                when(engineCore.getMachineId()).thenReturn("test-group.test-machine");
                when(engineCore.getGroupName()).thenReturn("test-group");
                when(engineCore.getMachineName()).thenReturn("test-machine");
                when(engineCore.getDispatcher())
                                .thenReturn(mock(org.sokybot.engine.api.IDispatcher.class));

                mockBundleContext = OSGiTestUtils.createMockBundleContext();

                registry = new ActuatorRegistry(
                                workflowRegistry, workflowContext, engineCore, Collections.emptyList(),
                                mockBundleContext);
        }

        @Test
        @DisplayName("Should initialize injected actuators")
        void testInitializeInjectedActuators() throws BundleException {
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

                // Re-create registry with actuators
                registry = new ActuatorRegistry(
                                workflowRegistry, workflowContext, engineCore, Arrays.asList(actuator1, actuator2),
                                mockBundleContext);

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

                // Re-create registry with failing actuator
                registry = new ActuatorRegistry(
                                workflowRegistry, workflowContext, engineCore,
                                Collections.singletonList(failingActuator), mockBundleContext);

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

                // Re-create registry with actuators
                registry = new ActuatorRegistry(
                                workflowRegistry, workflowContext, engineCore, Arrays.asList(actuator1, actuator2),
                                mockBundleContext);

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
        @DisplayName("Should handle null actuator list gracefully")
        void testNullActuatorList() {
                ActuatorRegistry registryWithNullList = new ActuatorRegistry(
                                workflowRegistry, workflowContext, engineCore, null, mockBundleContext);

                // Should not throw exception
                assertDoesNotThrow(() -> {
                        registryWithNullList.initializeActuators();
                });
        }
}
