package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.ServiceUnavailableException;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.proxy.IProxyConnection;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of workflow context.
 * Provides access to game model, dispatcher, and context data.
 */
public class WorkflowContextImpl implements IWorkflowContext {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowContextImpl.class);
    
    private final IGameModel gameModel;
    private final IDispatcher dispatcher;
    private final IProxyConnection proxyConnection;
    private final String machineId;
    private final String groupName;
    private final String machineName;
    private final BundleContext bundleContext;
    
    // State-local data (cleared when entering WAITING)
    private final Map<String, Object> stateData = new ConcurrentHashMap<>();
    
    // Persistent data (survives across cycles)
    private final Map<String, Object> persistentData = new ConcurrentHashMap<>();
    
    // Current state tracking
    private volatile String currentStateName;
    private final Map<Class<?>, ServiceTracker<?, ?>> serviceTrackers = new ConcurrentHashMap<>();
    
    public WorkflowContextImpl(IGameModel gameModel, IDispatcher dispatcher, IProxyConnection proxyConnection,
                              String groupName, String machineName, BundleContext bundleContext) {
        if (gameModel == null) {
            throw new IllegalArgumentException("Game model cannot be null");
        }
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher cannot be null");
        }
        if (proxyConnection == null) {
            throw new IllegalArgumentException("Proxy connection cannot be null");
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        if (machineName == null || machineName.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine name cannot be null or empty");
        }
        
        this.gameModel = gameModel;
        this.dispatcher = dispatcher;
        this.proxyConnection = proxyConnection;
        this.groupName = groupName;
        this.machineName = machineName;
        this.machineId = groupName + "." + machineName;
        this.bundleContext = bundleContext;
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
    public IProxyConnection getProxyConnection() {
        return proxyConnection;
    }

    @Override
    public String getCurrentStateName() {
        return currentStateName;
    }
    
    public void setCurrentStateName(String stateName) {
        this.currentStateName = stateName;
    }
    
    @Override
    public Map<String, Object> getStateData() {
        return stateData;
    }
    
    @Override
    public Map<String, Object> getPersistentData() {
        return persistentData;
    }
    
    /**
     * Clears state-local data.
     * Called when entering WAITING state.
     */
    public void clearStateData() {
        stateData.clear();
    }
    
    @Override
    public void log(String level, String message, Object... args) {
        String formattedMessage = String.format("[Machine: %s] [State: %s] %s", 
                                                machineId, currentStateName != null ? currentStateName : "N/A", 
                                                String.format(message, args));
        
        switch (level.toUpperCase()) {
            case "DEBUG":
                log.debug(formattedMessage);
                break;
            case "INFO":
                log.info(formattedMessage);
                break;
            case "WARN":
                log.warn(formattedMessage);
                break;
            case "ERROR":
                log.error(formattedMessage);
                break;
            default:
                log.info(formattedMessage);
        }
    }
    
    @Override
    public String getMachineId() {
        return machineId;
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
        if (serviceClass == null) {
            throw new IllegalArgumentException("serviceClass cannot be null");
        }
        if (bundleContext == null) {
            throw new ServiceUnavailableException("BundleContext unavailable for service " + serviceClass.getName());
        }
        ServiceTracker<T, T> tracker = (ServiceTracker<T, T>) serviceTrackers.computeIfAbsent(serviceClass, cls -> {
            ServiceTracker<T, T> created = new ServiceTracker<>(bundleContext, serviceClass, null);
            created.open();
            return created;
        });
        T service = tracker.getService();
        if (service == null) {
            throw new ServiceUnavailableException("Service unavailable: " + serviceClass.getName());
        }
        return service;
    }

    @Override
    public <T> Optional<T> getServiceOptional(Class<T> serviceClass) {
        try {
            return Optional.ofNullable(getService(serviceClass));
        } catch (ServiceUnavailableException e) {
            return Optional.empty();
        }
    }

    public void close() {
        serviceTrackers.values().forEach(tracker -> {
            try {
                tracker.close();
            } catch (Exception ignored) {
            }
        });
        serviceTrackers.clear();
    }
}
