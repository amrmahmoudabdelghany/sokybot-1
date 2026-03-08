package org.sokybot.engine.test.util.mocks;

import java.util.HashMap;
import java.util.Map;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.gamemodel.IGameModel;

/**
 * Mock IActuatorContext for offline script testing.
 * Provides a configurable in-memory context with MockDispatcher, MockGameModel,
 * and a stub service registry.
 *
 * <pre>
 * def ctx = new MockActuatorContext("test", "bot1")
 *     .withConnected(true)
 *     .withMockTrainer(12345)
 *     .withService(ISettingsRegistry, new StubSettingsRegistry())
 *
 * def login = new Login()
 * login.initialize(ctx)
 * assert ctx.dispatcher.serverPacketCount > 0
 * </pre>
 */
public class MockActuatorContext implements IActuatorContext {

    private final String groupName;
    private final String machineName;
    private final MockDispatcher dispatcher;
    private final MockGameModel gameModel;
    private final MockWorkflowRegistry workflowRegistry;
    private final Map<Class<?>, Object> services = new HashMap<>();

    public MockActuatorContext(String groupName, String machineName) {
        this.groupName = groupName;
        this.machineName = machineName;
        this.dispatcher = new MockDispatcher();
        this.gameModel = new MockGameModel();
        this.workflowRegistry = new MockWorkflowRegistry();
    }

    public MockActuatorContext withConnected(boolean connected) {
        dispatcher.withConnected(connected);
        return this;
    }

    public MockActuatorContext withMockTrainer(int uniqueId) {
        gameModel.withMockTrainer(uniqueId);
        return this;
    }

    public <T> MockActuatorContext withService(Class<T> type, T instance) {
        services.put(type, instance);
        return this;
    }

    @Override
    public IWorkflowRegistry getWorkflowRegistry() {
        return workflowRegistry;
    }

    @Override
    public IGameModel getGameModel() {
        return gameModel;
    }

    @Override
    public IDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    public String getMachineId() {
        return groupName + "." + machineName;
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public String getMachineName() {
        return machineName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }

    public MockDispatcher getTestDispatcher() {
        return dispatcher;
    }

    public MockGameModel getTestGameModel() {
        return gameModel;
    }

    public MockWorkflowRegistry getTestWorkflowRegistry() {
        return workflowRegistry;
    }
}
