package org.sokybot.engine.api.extension;

import org.sokybot.engine.api.IDispatcher;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.gamemodel.IGameModel;
import java.util.Map;

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
     * Gets the machine ID.
     * Format: "groupName.machineName"
     * 
     * @return The machine ID
     */
    String getMachineId();

    /**
     * Gets the group name.
     * 
     * @return The group name
     */
    String getGroupName();

    /**
     * Gets the machine name.
     * 
     * @return The machine name
     */
    String getMachineName();

    /**
     * Gets an OSGi service.
     * 
     * @param serviceClass The service interface class
     * @return The service instance, or null if not found
     * @param <T> The service type
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * Gets actuator-private session data.
     * Cleared on disconnect/shutdown boundaries.
     */
    Map<String, Object> getSessionData();

    /**
     * Gets typed durable settings facade.
     */
    ISettings getSettings();

    /**
     * Gets raw durable settings map (advanced use only).
     */
    Map<String, Object> getSettingsData();
}
