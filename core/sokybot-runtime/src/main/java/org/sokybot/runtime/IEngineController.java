package org.sokybot.runtime;

import org.sokybot.engine.IEngine;

/**
 * Interface for components that provide access to engine control.
 */
public interface IEngineController {

    /**
     * Gets the engine instance.
     * 
     * @return The engine instance.
     */
    IEngine getEngine();

    /**
     * Checks if the engine (and thus the machine) is running.
     * 
     * @return True if running, false otherwise.
     */
    boolean isRunning();
}
