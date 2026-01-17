package org.sokybot.engine.api.extension;

/**
 * Interface for actuator bundles to implement.
 * Actuators are discovered via OSGi service registration.
 * 
 * Actuators define cycles that participate in the engine workflow.
 */
public interface IActuator {
    
    /**
     * Gets the unique name of this actuator.
     * Used for logging and identification.
     * 
     * @return Actuator name (e.g., "connector", "login", "training")
     */
    String getName();
    
    /**
     * Initializes this actuator.
     * Called when engine is created, before cycle starts.
     * Actuator should register cycles here.
     * 
     * @param context The actuator context
     * @throws BundleException if initialization fails
     */
    void initialize(IActuatorContext context);
    
    /**
     * Shuts down this actuator.
     * Called when engine is stopped.
     * Actuator should clean up resources here.
     * 
     * @param context The actuator context
     */
    default void shutdown(IActuatorContext context) {
        // Optional: Override for cleanup
    }
}
