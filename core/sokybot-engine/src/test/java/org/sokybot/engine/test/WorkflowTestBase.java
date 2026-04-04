package org.sokybot.engine.test;

import org.mockito.Mockito;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.test.util.mocks.MockDispatcher;
import org.sokybot.engine.test.util.mocks.MockGameModel;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.proxy.IProxyConnection;

/**
 * Base class for workflow tests.
 * Provides mock workflow context creation and test helpers.
 */
public abstract class WorkflowTestBase {
    
    protected MockGameModel mockGameModel;
    protected MockDispatcher mockDispatcher;
    protected String testGroupName = "test-group";
    protected String testMachineName = "test-machine";
    
    /**
     * Sets up test fixtures before each test.
     */
    protected void setUp() {
        mockGameModel = new MockGameModel();
        mockDispatcher = new MockDispatcher();
    }
    
    /**
     * Creates a test workflow context.
     * 
     * @return A new WorkflowContextImpl instance configured for testing
     */
    protected WorkflowContextImpl createWorkflowContext() {
        return createWorkflowContext(mockGameModel, mockDispatcher, testGroupName, testMachineName);
    }
    
    /**
     * Creates a test workflow context with custom dependencies.
     * 
     * @param gameModel The game model to use
     * @param dispatcher The dispatcher to use
     * @param groupName The group name
     * @param machineName The machine name
     * @return A new WorkflowContextImpl instance
     */
    protected WorkflowContextImpl createWorkflowContext(
            IGameModel gameModel,
            MockDispatcher dispatcher,
            String groupName,
            String machineName) {
        return new WorkflowContextImpl(
            gameModel,
            dispatcher,
            Mockito.mock(IProxyConnection.class),
            groupName,
            machineName,
            Mockito.mock(org.osgi.framework.BundleContext.class));
    }
    
    /**
     * Tears down test fixtures after each test.
     */
    protected void tearDown() {
        // Cleanup if needed
    }
}
