package org.sokybot.engine.test;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.engine.test.util.mocks.MockDispatcher;
import org.sokybot.engine.test.util.mocks.MockGameModel;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.settings.ISettingsManager;
import org.sokybot.settings.Settings;

/**
 * Base class for workflow tests.
 * Provides mock workflow context creation and test helpers.
 */
public abstract class WorkflowTestBase {
    
    @Mock
    protected ISettingsManager settingsManager;
    
    protected MockGameModel mockGameModel;
    protected MockDispatcher mockDispatcher;
    protected Settings testSettings;
    
    /**
     * Sets up test fixtures before each test.
     */
    protected void setUp() {
        mockGameModel = new MockGameModel();
        mockDispatcher = new MockDispatcher();
        
        testSettings = new Settings("test-machine", "test-group", "test-machine");
        
        if (settingsManager == null) {
            settingsManager = Mockito.mock(ISettingsManager.class);
        }
    }
    
    /**
     * Creates a test workflow context.
     * 
     * @return A new WorkflowContextImpl instance configured for testing
     */
    protected WorkflowContextImpl createWorkflowContext() {
        return createWorkflowContext(mockGameModel, mockDispatcher, testSettings);
    }
    
    /**
     * Creates a test workflow context with custom dependencies.
     * 
     * @param gameModel The game model to use
     * @param dispatcher The dispatcher to use
     * @param settings The settings to use
     * @return A new WorkflowContextImpl instance
     */
    protected WorkflowContextImpl createWorkflowContext(
            IGameModel gameModel,
            MockDispatcher dispatcher,
            Settings settings) {
        return new WorkflowContextImpl(
            gameModel,
            dispatcher,
            settings,
            settingsManager,
            "test-machine");
    }
    
    /**
     * Tears down test fixtures after each test.
     */
    protected void tearDown() {
        // Cleanup if needed
    }
}
