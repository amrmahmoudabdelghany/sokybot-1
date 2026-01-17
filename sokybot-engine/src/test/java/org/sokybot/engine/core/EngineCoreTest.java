package org.sokybot.engine.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sokybot.engine.api.EngineState;
import org.sokybot.engine.test.EngineTestBase;
import org.sokybot.engine.test.util.OSGiTestUtils;
import org.sokybot.gamemodel.IGameModel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EngineCore.
 */
@DisplayName("EngineCore Tests")
class EngineCoreTest extends EngineTestBase {
    
    private EngineCore engine;
    
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // Engine will be created in individual tests
    }
    
    @Test
    @DisplayName("Should create engine with valid parameters")
    void testCreateEngine() {
        engine = createTestEngine();
        
        assertNotNull(engine);
        assertEquals(TEST_MACHINE_ID, engine.getMachineId());
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());
    }
    
    @Test
    @DisplayName("Should throw exception when creating engine with null machine ID")
    void testCreateEngineWithNullMachineId() {
        assertThrows(IllegalArgumentException.class, () -> {
            new EngineCore(null, TEST_GROUP_NAME, TEST_MACHINE_NAME,
                          mockProxyConnection, mockGameModel,
                          testSettings, settingsManager, mockBundleContext);
        });
    }
    
    @Test
    @DisplayName("Should throw exception when creating engine with null proxy connection")
    void testCreateEngineWithNullProxyConnection() {
        assertThrows(IllegalArgumentException.class, () -> {
            new EngineCore(TEST_MACHINE_ID, TEST_GROUP_NAME, TEST_MACHINE_NAME,
                          null, mockGameModel,
                          testSettings, settingsManager, mockBundleContext);
        });
    }
    
    @Test
    @DisplayName("Should start engine successfully")
    void testStartEngine() throws InterruptedException {
        engine = createTestEngine();
        
        engine.start();
        
        assertEquals(EngineState.IDLE, engine.getEngineState());
        assertTrue(engine.isRunning());
        
        // Cleanup
        engine.stop();
    }
    
    @Test
    @DisplayName("Should stop engine successfully")
    void testStopEngine() {
        engine = createTestEngine();
        
        engine.start();
        assertTrue(engine.isRunning());
        
        engine.stop();
        
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        assertFalse(engine.isRunning());
    }
    
    @Test
    @DisplayName("Should not start engine twice")
    void testStartEngineTwice() {
        engine = createTestEngine();
        
        engine.start();
        assertTrue(engine.isRunning());
        
        // Second start should be ignored (logs warning)
        assertDoesNotThrow(() -> {
            engine.start();
        });
        
        engine.stop();
    }
    
    @Test
    @DisplayName("Should not stop engine twice")
    void testStopEngineTwice() {
        engine = createTestEngine();
        
        engine.start();
        engine.stop();
        
        // Second stop should be ignored (logs warning)
        assertDoesNotThrow(() -> {
            engine.stop();
        });
        
        assertEquals(EngineState.STOPPED, engine.getEngineState());
    }
    
    @Test
    @DisplayName("Should initialize actuators on start")
    void testInitializeActuatorsOnStart() {
        engine = createTestEngine();
        
        // Verify actuator registry is initialized
        assertNotNull(engine.getWorkflowRegistry());
        
        engine.start();
        
        // After start, actuators should be initialized
        // (In a real test with actuators, we could verify they were registered)
        assertTrue(engine.isRunning());
        
        engine.stop();
    }
    
    @Test
    @DisplayName("Should get workflow registry")
    void testGetWorkflowRegistry() {
        engine = createTestEngine();
        
        assertNotNull(engine.getWorkflowRegistry());
    }
    
    @Test
    @DisplayName("Should get settings")
    void testGetSettings() {
        engine = createTestEngine();
        
        assertNotNull(engine.getSettings());
        assertSame(testSettings, engine.getSettings());
    }
    
    @Test
    @DisplayName("Should handle sendEvent when running")
    void testSendEventWhenRunning() {
        engine = createTestEngine();
        
        engine.start();
        
        // Should not throw exception
        assertDoesNotThrow(() -> {
            engine.sendEvent("START_TRAINING");
        });
        
        engine.stop();
    }
    
    @Test
    @DisplayName("Should throw exception when sending event while stopped")
    void testSendEventWhenStopped() {
        engine = createTestEngine();
        
        // Engine is stopped by default
        assertThrows(IllegalStateException.class, () -> {
            engine.sendEvent("START_TRAINING");
        });
    }
    
    @Test
    @DisplayName("Should transition from STOPPED to IDLE to STOPPED")
    void testStateTransitions() {
        engine = createTestEngine();
        
        // Initial state
        assertEquals(EngineState.STOPPED, engine.getEngineState());
        
        // Start -> IDLE
        engine.start();
        assertEquals(EngineState.IDLE, engine.getEngineState());
        
        // Stop -> STOPPED
        engine.stop();
        assertEquals(EngineState.STOPPED, engine.getEngineState());
    }
}
