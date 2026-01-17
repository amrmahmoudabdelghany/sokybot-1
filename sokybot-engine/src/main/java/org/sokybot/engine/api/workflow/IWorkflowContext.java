package org.sokybot.engine.api.workflow;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.settings.Settings;
import org.sokybot.settings.ISettingsManager;
import java.util.Map;

/**
 * Context provided to guards and actions during workflow execution.
 * Provides access to game state, dispatcher, settings, and context data.
 */
public interface IWorkflowContext {
    
    /**
     * Gets the game model (shared state).
     * Use to read current game state.
     * 
     * @return The game model instance
     */
    IGameModel getGameModel();
    
    /**
     * Gets the packet dispatcher for sending packets to server.
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
     * Gets the current orthogonal state name.
     * 
     * @return The current state name, or null if not in a state
     */
    String getCurrentStateName();
    
    /**
     * Gets state-local context data.
     * This map is cleared when returning to WAITING state.
     * Use for passing data between states in a single cycle iteration.
     * 
     * @return Mutable map for state-local data
     */
    Map<String, Object> getStateData();
    
    /**
     * Gets persistent context data.
     * This map persists across cycles.
     * Use for data that should survive cycle restarts.
     * 
     * @return Mutable map for persistent data
     */
    Map<String, Object> getPersistentData();
    
    /**
     * Logs a message with workflow context.
     * Includes state name, machine ID, etc.
     * 
     * @param level The log level (e.g., "INFO", "DEBUG", "ERROR")
     * @param message The log message
     * @param args Optional arguments for message formatting
     */
    void log(String level, String message, Object... args);

    /**
     * Gets the machine ID.
     * 
     * @return The machine ID
     */
    String getMachineId();
}
