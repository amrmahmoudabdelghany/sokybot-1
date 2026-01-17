package org.sokybot.engine.core;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.settings.Settings;
import org.sokybot.settings.ISettingsManager;

/**
 * Implementation of actuator context.
 * Provides access to engine infrastructure for actuators.
 */
public class ActuatorContextImpl implements IActuatorContext {
    
    private final IWorkflowRegistry workflowRegistry;
    private final IGameModel gameModel;
    private final IDispatcher dispatcher;
    private final Settings settings;
    private final ISettingsManager settingsManager;
    private final String machineId;
    
    public ActuatorContextImpl(IWorkflowRegistry workflowRegistry,
                              IGameModel gameModel,
                              IDispatcher dispatcher,
                              Settings settings,
                              ISettingsManager settingsManager,
                              String machineId) {
        if (workflowRegistry == null) {
            throw new IllegalArgumentException("Workflow registry cannot be null");
        }
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
        
        this.workflowRegistry = workflowRegistry;
        this.gameModel = gameModel;
        this.dispatcher = dispatcher;
        this.settings = settings;
        this.settingsManager = settingsManager;
        this.machineId = machineId;
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
    public Settings getSettings() {
        return settings;
    }
    
    @Override
    public ISettingsManager getSettingsManager() {
        return settingsManager;
    }
    
    @Override
    public String getMachineId() {
        return machineId;
    }
}
