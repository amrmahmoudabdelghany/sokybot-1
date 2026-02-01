package org.sokybot.engine.plugin;

import java.nio.file.Path;
import java.util.Optional;
import org.sokybot.engine.api.extension.IActuator;

/**
 * Service for loading and managing script-based actuators.
 */
public interface IScriptActuatorLoader {

    /**
     * Scan the scripts directory and load all valid actuators.
     * 
     * @return number of scripts loaded
     */
    int scanAndLoad();

    /**
     * Load a specific script from a file.
     * 
     * @param scriptFile path to the .groovy script
     * @return the loaded actuator, or empty if failed
     */
    Optional<IActuator> loadScript(Path scriptFile);

    /**
     * Unload a scripted actuator by name.
     * 
     * @param name actuator name
     * @return true if unloaded
     */
    boolean unloadScript(String name);

    /**
     * Get the base directory where scripts are stored.
     */
    Path getScriptsDirectory();

    /**
     * Trigger a reload of all scripts.
     */
    void reloadAll();
}
