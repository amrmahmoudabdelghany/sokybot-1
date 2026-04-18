package org.sokybot.login.workflow.builtin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.proxy.IProxyConnection;

/**
 * Minimal {@link IWorkflowContext} over {@link IActuatorContext} for protocol graceful shutdown on actuator stop.
 */
final class ActuatorShutdownBridge implements IWorkflowContext {

    private static final String DISPATCHER_IMPL_CLASS_NAME = "org.sokybot.engine.core.dispatcher.DispatcherImpl";

    private final IActuatorContext actuatorContext;
    private final Map<String, Object> stateData = new ConcurrentHashMap<>();
    private final Map<String, Object> persistentData = new ConcurrentHashMap<>();

    ActuatorShutdownBridge(IActuatorContext actuatorContext) {
        this.actuatorContext = actuatorContext;
    }

    @Override
    public IGameModel getGameModel() {
        return actuatorContext.getGameModel();
    }

    @Override
    public IDispatcher getDispatcher() {
        return actuatorContext.getDispatcher();
    }

    @Override
    public IProxyConnection getProxyConnection() {
        IDispatcher dispatcher = getDispatcher();
        if (dispatcher == null) {
            return null;
        }
        try {
            Class<?> dispatcherClass = dispatcher.getClass();
            if (DISPATCHER_IMPL_CLASS_NAME.equals(dispatcherClass.getName())) {
                java.lang.reflect.Field field = dispatcherClass.getDeclaredField("proxyConnection");
                field.setAccessible(true);
                Object value = field.get(dispatcher);
                if (value instanceof IProxyConnection) {
                    return (IProxyConnection) value;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Override
    public String getCurrentStateName() {
        return null;
    }

    @Override
    public Map<String, Object> getStateData() {
        return stateData;
    }

    @Override
    public Map<String, Object> getPersistentData() {
        return persistentData;
    }

    @Override
    public void log(String level, String message, Object... args) {
        // Not used during shutdown bridge
    }

    @Override
    public String getMachineId() {
        return actuatorContext.getMachineId();
    }

    @Override
    public String getGroupName() {
        return actuatorContext.getGroupName();
    }

    @Override
    public String getMachineName() {
        return actuatorContext.getMachineName();
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        if (serviceClass == null) {
            return null;
        }
        return actuatorContext.getService(serviceClass);
    }

    @Override
    public <T> Optional<T> getServiceOptional(Class<T> serviceClass) {
        return Optional.ofNullable(getService(serviceClass));
    }
}
