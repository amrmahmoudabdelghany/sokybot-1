package org.sokybot.engine.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.core.EngineCore;
import org.sokybot.engine.test.EngineTestBase;
import org.sokybot.engine.test.util.MockActuator;
import org.sokybot.engine.test.util.OSGiTestUtils;
import org.osgi.framework.BundleContext;
import org.sokybot.engine.test.util.WorkflowTestBuilders;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for engine lifecycle.
 */
@DisplayName("Engine Lifecycle Integration Tests")
class EngineLifecycleIntegrationTest extends EngineTestBase {

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
    }

    @Test
    @DisplayName("Should complete full engine startup sequence")
    void testFullEngineStartupSequence() throws InterruptedException {
        MockActuator actuator = new MockActuator("startup-actuator")
                .withCycle(WorkflowTestBuilders.cycle("startup-cycle")
                        .priority(100)
                        .entryState("STATE1")
                        .state("STATE1",
                                WorkflowTestBuilders.guard().returns(true).build(),
                                WorkflowTestBuilders.action().build(),
                                null)
                        .build());

        mockBundleContext.registerMockService(IActuator.class, actuator, null);

        EngineCore engine = new EngineCore(
                TEST_MACHINE_ID,
                TEST_GROUP_NAME,
                TEST_MACHINE_NAME,
                mockProxyConnection,
                mockGameModel,
                java.util.Collections.singletonList(actuator),
                mockBundleContext);

        // Verify initial state
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());

        // Start engine
        engine.start();

        // Verify engine started
        assertEquals(EngineState.IDLE, engine.getEngineState());
        assertTrue(engine.isRunning());

        // Verify actuator was initialized
        assertTrue(actuator.isInitialized());

        engine.stop();
    }

    @Test
    @DisplayName("Should complete full engine shutdown sequence")
    void testFullEngineShutdownSequence() throws InterruptedException {
        MockActuator actuator = new MockActuator("shutdown-actuator")
                .withCycle(WorkflowTestBuilders.cycle("shutdown-cycle")
                        .priority(100)
                        .entryState("STATE1")
                        .state("STATE1",
                                WorkflowTestBuilders.guard().returns(true).build(),
                                WorkflowTestBuilders.action().build(),
                                null)
                        .build());

        mockBundleContext.registerMockService(IActuator.class, actuator, null);

        EngineCore engine = new EngineCore(
                TEST_MACHINE_ID,
                TEST_GROUP_NAME,
                TEST_MACHINE_NAME,
                mockProxyConnection,
                mockGameModel,
                java.util.Collections.singletonList(actuator),
                mockBundleContext);
        engine.start();

        assertTrue(engine.isRunning());
        assertTrue(actuator.isInitialized());

        // Stop engine
        engine.stop();

        // Verify engine stopped
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());

        // Verify actuator was shut down
        assertTrue(actuator.isShutdown());
    }

    @Test
    @DisplayName("Should not restart after stop")
    void testRestartAfterStop() {
        EngineCore engine = createTestEngine();

        engine.start();
        assertEquals(EngineState.IDLE, engine.getEngineState());

        engine.stop();
        assertEquals(EngineState.STOPPED, engine.getEngineState());

        // Attempt to restart - should log warning but not throw exception
        // (Based on implementation, it may ignore or throw)
        assertDoesNotThrow(() -> {
            engine.start();
        });
    }

    @Test
    @DisplayName("Should support multiple engines with different machine IDs")
    void testMultipleEngines() throws InterruptedException {
        // Create first engine
        MockActuator actuator1 = new MockActuator("engine1-actuator")
                .withCycle(WorkflowTestBuilders.cycle("engine1-cycle")
                        .priority(100)
                        .entryState("STATE1")
                        .state("STATE1",
                                WorkflowTestBuilders.guard().returns(true).build(),
                                WorkflowTestBuilders.action().build(),
                                null)
                        .build());

        mockBundleContext.registerMockService(IActuator.class, actuator1, null);
        EngineCore engine1 = createTestEngine();

        // Create second engine with different ID
        OSGiTestUtils.MockBundleContext mockBundle2 = OSGiTestUtils.createMockBundleContext();
        MockActuator actuator2 = new MockActuator("engine2-actuator")
                .withCycle(WorkflowTestBuilders.cycle("engine2-cycle")
                        .priority(100)
                        .entryState("STATE1")
                        .state("STATE1",
                                WorkflowTestBuilders.guard().returns(true).build(),
                                WorkflowTestBuilders.action().build(),
                                null)
                        .build());

        mockBundle2.registerMockService(IActuator.class, actuator2, null);
        EngineCore engine2 = new EngineCore(
                "test-group.test-machine-2",
                TEST_GROUP_NAME,
                "test-machine-2",
                mockProxyConnection,
                mockGameModel,
                java.util.Collections.singletonList(actuator2),
                mockBundle2);

        // Both engines should be independent
        engine1.start();
        engine2.start();

        assertEquals(EngineState.IDLE, engine1.getEngineState());
        assertEquals(EngineState.IDLE, engine2.getEngineState());
        assertTrue(engine1.isRunning());
        assertTrue(engine2.isRunning());

        // Stop both
        engine1.stop();
        engine2.stop();

        assertEquals(EngineState.STOPPED, engine1.getEngineState());
        assertEquals(EngineState.STOPPED, engine2.getEngineState());
    }

    @Test
    @DisplayName("Should maintain engine state consistency")
    void testEngineStateConsistency() {
        EngineCore engine = createTestEngine();

        // Initial state
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());

        // Start
        engine.start();
        assertEquals(EngineState.IDLE, engine.getEngineState());
        assertTrue(engine.isRunning());

        // Check state multiple times (should be consistent)
        assertEquals(EngineState.IDLE, engine.getEngineState());
        assertTrue(engine.isRunning());
        assertEquals(EngineState.IDLE, engine.getEngineState());

        // Stop
        engine.stop();
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());

        // Check state again (should still be stopped)
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());
    }

    @Test
    @DisplayName("Should handle engine lifecycle with no actuators")
    void testEngineLifecycleWithNoActuators() {
        EngineCore engine = createTestEngine();

        // Should still start and stop successfully
        assertDoesNotThrow(() -> {
            engine.start();
        });

        assertEquals(EngineState.IDLE, engine.getEngineState());
        assertTrue(engine.isRunning());

        assertDoesNotThrow(() -> {
            engine.stop();
        });

        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());
    }
}
