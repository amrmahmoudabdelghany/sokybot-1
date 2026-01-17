package org.sokybot.engine.core.workflow;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.settings.Settings;
import org.sokybot.settings.ISettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of workflow context.
 * Provides access to game model, dispatcher, settings, and context data.
 */
public class WorkflowContextImpl implements IWorkflowContext {
    
    private static final Logger log = LoggerFactory.getLogger(WorkflowContextImpl.class);
    
    private final IGameModel gameModel;
    private final IDispatcher dispatcher;
    private final Settings settings;
    private final ISettingsManager settingsManager;
    private final String machineId;
    
    // State-local data (cleared when entering WAITING)
    private final Map<String, Object> stateData = new ConcurrentHashMap<>();
    
    // Persistent data (survives across cycles)
    private final Map<String, Object> persistentData = new ConcurrentHashMap<>();
    
    // Current state tracking
    private volatile String currentStateName;
    
    public WorkflowContextImpl(IGameModel gameModel, IDispatcher dispatcher,
                              Settings settings, ISettingsManager settingsManager,
                              String machineId) {
        if (gameModel == null) {
            throw new IllegalArgumentException("Game model cannot be null");
        }
        if (dispatcher == null) {
            throw new IllegalArgumentException("Dispatcher cannot be null");
        }
        if (settings == null) {
            throw new IllegalArgumentException("Settings cannot be null");
        }
        if (settingsManager == null) {
            throw new IllegalArgumentException("Settings manager cannot be null");
        }
        if (machineId == null || machineId.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine ID cannot be null or empty");
        }
        
        this.gameModel = gameModel;
        this.dispatcher = dispatcher;
        this.settings = settings;
        this.settingsManager = settingsManager;
        this.machineId = machineId;
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
    public Settings getSettings() {
        return settings;
    }
    
    @Override
    public ISettingsManager getSettingsManager() {
        return settingsManager;
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
    
    public String getMachineId() {
        return machineId;
    }
}
