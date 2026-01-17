package org.sokybot.engine.api.extension;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.settings.Settings;
import org.sokybot.settings.ISettingsManager;

/**
 * Context provided to actuators during initialization.
 * Actuators use this to register cycles and access infrastructure.
 */
public interface IActuatorContext {
    
    /**
     * Gets the workflow registry for registering cycles.
     * 
     * @return The workflow registry
     */
    IWorkflowRegistry getWorkflowRegistry();
    
    /**
     * Gets the game model (shared state).
     * 
     * @return The game model instance
     */
    IGameModel getGameModel();
    
    /**
     * Gets the packet dispatcher for sending packets.
     * 
     * @return The dispatcher instance
     */
    IDispatcher getDispatcher();
    
    /**
     * Gets the settings for this machine.
     * 
     * @return The settings object
     */
    Settings getSettings();
    
    /**
     * Gets the settings manager for this machine.
     * 
     * @return The settings manager
     */
    ISettingsManager getSettingsManager();
    
    /**
     * Gets the machine ID.
     * Format: "groupName.machineName"
     * 
     * @return The machine ID
     */
    String getMachineId();
}
