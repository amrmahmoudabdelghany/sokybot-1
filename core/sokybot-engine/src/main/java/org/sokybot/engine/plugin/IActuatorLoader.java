package org.sokybot.engine.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.sokybot.engine.api.extension.IActuator;

/**
 * Service for loading actuator plugins from external sources.
 * 
 * Actuators can be loaded from:
 * - JAR files containing actuator.yaml manifest
 * - Directories containing compiled classes
 */
public interface IActuatorLoader {

    /**
     * Load all actuators from a directory.
     * Scans for JAR files and directories containing actuators.
     * 
     * @param directory the directory to scan
     * @return number of actuators loaded
     */
    int loadFromDirectory(Path directory);

    /**
     * Load a single actuator from a JAR file.
     * 
     * @param jarFile the JAR file
     * @return the loaded actuator, or empty if failed
     */
    Optional<IActuator> loadFromJar(Path jarFile);

    /**
     * Get all loaded actuators.
     * 
     * @return list of loaded actuators
     */
    List<LoadedActuator> getLoadedActuators();

    /**
     * Unload an actuator by name.
     * 
     * @param name the actuator name
     * @return true if unloaded, false if not found
     */
    boolean unload(String name);

    /**
     * Get a specific loaded actuator by name.
     * 
     * @param name the actuator name
     * @return the loaded actuator, or empty if not found
     */
    Optional<LoadedActuator> getActuator(String name);

    /**
     * Reload all actuators from the configured plugin directory.
     * 
     * @return number of actuators loaded
     */
    int reloadAll();

    /**
     * Get the plugin directory path.
     * 
     * @return the plugin directory
     */
    Path getPluginDirectory();
}
