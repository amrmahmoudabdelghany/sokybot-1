package org.sokybot.engine.test;

import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.sokybot.engine.core.EngineCore;
import org.sokybot.engine.test.util.OSGiTestUtils;
import org.sokybot.engine.test.util.mocks.MockDispatcher;
import org.sokybot.engine.test.util.mocks.MockGameModel;
import org.sokybot.engine.test.util.mocks.MockProxyConnection;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.proxy.IProxyConnection;

/**
 * Base class for engine tests.
 * Provides common test setup (mock services, context creation).
 */
public abstract class EngineTestBase {

    protected static final String TEST_MACHINE_ID = "test-group.test-machine";
    protected static final String TEST_GROUP_NAME = "test-group";
    protected static final String TEST_MACHINE_NAME = "test-machine";

    protected OSGiTestUtils.MockBundleContext mockBundleContext;
    protected MockProxyConnection mockProxyConnection;
    protected MockGameModel mockGameModel;
    protected MockDispatcher mockDispatcher;

    /**
     * Sets up test fixtures before each test.
     * Subclasses can override to add custom setup.
     */
    protected void setUp() {
        // Create mock bundle context
        mockBundleContext = OSGiTestUtils.createMockBundleContext();

        // Create mock proxy connection
        mockProxyConnection = new MockProxyConnection().withConnected(false);

        // Create mock game model
        mockGameModel = new MockGameModel();

        // Create mock dispatcher
        mockDispatcher = new MockDispatcher().withConnected(false);
    }

    /**
     * Creates a test engine instance.
     * 
     * @return A new EngineCore instance configured for testing
     */
    protected EngineCore createTestEngine() {
        return createTestEngine(mockGameModel);
    }

    /**
     * Creates a test engine instance with the given game model.
     * 
     * @param gameModel The game model to use
     * @return A new EngineCore instance configured for testing
     */
    protected EngineCore createTestEngine(IGameModel gameModel) {
        return new EngineCore(
                TEST_MACHINE_ID,
                TEST_GROUP_NAME,
                TEST_MACHINE_NAME,
                mockProxyConnection,
                gameModel,
                java.util.Collections.emptyList(),
                mockBundleContext);
    }

    /**
     * Tears down test fixtures after each test.
     * Subclasses can override to add custom cleanup.
     */
    protected void tearDown() {
        if (mockBundleContext != null) {
            mockBundleContext.clearServices();
        }
    }
}
