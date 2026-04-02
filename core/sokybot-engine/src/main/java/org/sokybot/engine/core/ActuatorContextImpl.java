package org.sokybot.engine.core;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.extension.ISettings;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.gamemodel.IGameModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of actuator context.
 * Provides access to engine infrastructure for actuators.
 */
public class ActuatorContextImpl implements IActuatorContext {

    private final IWorkflowRegistry workflowRegistry;
    private final IGameModel gameModel;
    private final IDispatcher dispatcher;
    private final String machineId;
    private final String groupName;
    private final String machineName;
    private final org.osgi.framework.BundleContext bundleContext;
    private final Map<String, Object> sessionData = new ConcurrentHashMap<>();
    private final Map<String, Object> settingsData = new ConcurrentHashMap<>();
    private final ISettings settingsView = new MapBackedSettings();

    public ActuatorContextImpl(IWorkflowRegistry workflowRegistry,
            IGameModel gameModel,
            IDispatcher dispatcher,
            String groupName,
            String machineName,
            org.osgi.framework.BundleContext bundleContext) {
        if (workflowRegistry == null) {
            throw new IllegalArgumentException("Workflow registry cannot be null");
        }
        if (gameModel == null) {
            throw new IllegalArgumentException("Game model cannot be null");
        }
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher cannot be null");
        }
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }
        if (machineName == null || machineName.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine name cannot be null or empty");
        }

        this.workflowRegistry = workflowRegistry;
        this.gameModel = gameModel;
        this.dispatcher = dispatcher;
        this.groupName = groupName;
        this.machineName = machineName;
        this.machineId = groupName + "." + machineName;
        this.bundleContext = bundleContext;
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
    public <T> T getService(Class<T> serviceClass) {
        if (bundleContext == null) {
            return null;
        }
        org.osgi.framework.ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);
        if (ref != null) {
            return bundleContext.getService(ref);
        }
        return null;
    }

    @Override
    public Map<String, Object> getSessionData() {
        return sessionData;
    }

    @Override
    public ISettings getSettings() {
        return settingsView;
    }

    @Override
    public Map<String, Object> getSettingsData() {
        return settingsData;
    }

    void clearSessionData() {
        sessionData.clear();
    }

    private final class MapBackedSettings implements ISettings {
        @Override
        public String getString(String key, String defaultValue) {
            Object v = settingsData.get(key);
            return v == null ? defaultValue : String.valueOf(v);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            Object v = settingsData.get(key);
            if (v instanceof Number) return ((Number) v).intValue();
            try { return v == null ? defaultValue : Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return defaultValue; }
        }

        @Override
        public long getLong(String key, long defaultValue) {
            Object v = settingsData.get(key);
            if (v instanceof Number) return ((Number) v).longValue();
            try { return v == null ? defaultValue : Long.parseLong(String.valueOf(v)); } catch (Exception e) { return defaultValue; }
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            Object v = settingsData.get(key);
            if (v instanceof Boolean) return (Boolean) v;
            return v == null ? defaultValue : Boolean.parseBoolean(String.valueOf(v));
        }

        @Override
        public void put(String key, Object value) {
            if (value == null) settingsData.remove(key);
            else settingsData.put(key, value);
        }

        @Override
        public Map<String, Object> asMap() {
            return settingsData;
        }

        @Override
        public void save() {
            // SettingsRegistry-backed persistence can be wired later;
            // map remains durable within engine lifetime.
        }
    }
}
