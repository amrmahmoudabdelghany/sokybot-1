package org.sokybot.engine.core;

import org.osgi.framework.BundleContext;

import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.extension.BundleException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.engine.IEngine;
import org.sokybot.gamemodel.IGameModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;

/**
 * Registry for actuator bundles.
 * Discovers and initializes actuators via OSGi service registration.
 */
public class ActuatorRegistry {

    private static final Logger log = LoggerFactory.getLogger(ActuatorRegistry.class);

    private final IWorkflowRegistry workflowRegistry;
    private final IWorkflowContext workflowContext;
    private final IEngine engine;
    private final List<IActuator> availableActuators;
    private final BundleContext bundleContext;

    private final Map<String, IActuator> actuators = new ConcurrentHashMap<>();
    private final List<IActuatorContext> actuatorContexts = new CopyOnWriteArrayList<>();

    public ActuatorRegistry(IWorkflowRegistry workflowRegistry,
            IWorkflowContext workflowContext,
            IEngine engine,
            List<IActuator> availableActuators,
            BundleContext bundleContext) {
        this.workflowRegistry = workflowRegistry;
        this.workflowContext = workflowContext;
        this.engine = engine;
        this.availableActuators = availableActuators != null ? availableActuators : new ArrayList<>();
        this.bundleContext = bundleContext;
    }

    /**
     * Initializes actuators by discovering them via OSGi services.
     * Called when engine starts.
     */
    public void initializeActuators() {
        log.info("Initializing actuators for machine: {}", engine.getMachineId());

        if (availableActuators.isEmpty()) {
            log.warn("No actuators available for initialization");
            return;
        }

        for (IActuator actuator : availableActuators) {
            try {
                registerActuator(actuator);
            } catch (Exception e) {
                log.error("Failed to register actuator {}: {}", actuator.getName(), e.getMessage(), e);
            }
        }

        log.info("Actuator initialization complete for machine: {}", engine.getMachineId());
    }

    /**
     * Registers an actuator manually.
     * Used for built-in actuators or testing.
     * 
     * @param actuator The actuator to register
     */
    public void registerActuator(IActuator actuator) {
        if (actuator == null) {
            throw new IllegalArgumentException("Actuator cannot be null");
        }

        String actuatorName = actuator.getName();
        if (actuatorName == null || actuatorName.trim().isEmpty()) {
            throw new IllegalArgumentException("Actuator name cannot be null or empty");
        }

        if (actuators.containsKey(actuatorName)) {
            log.warn("Actuator '{}' already registered, replacing", actuatorName);
        }

        log.info("Registering actuator: {}", actuatorName);

        // Create actuator context
        ActuatorContextImpl context = new ActuatorContextImpl(
                workflowRegistry, workflowContext.getGameModel(),
                engine.getDispatcher(),
                engine.getGroupName(), engine.getMachineName(),
                bundleContext);

        actuatorContexts.add(context);

        // Initialize actuator
        try {
            actuator.initialize(context);
            actuators.put(actuatorName, actuator);
            log.info("Actuator '{}' initialized successfully", actuatorName);
        } catch (BundleException e) {
            log.error("Failed to initialize actuator '{}': {}", actuatorName, e.getMessage(), e);
            throw new IllegalStateException("Failed to initialize actuator: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error initializing actuator '{}': {}", actuatorName, e.getMessage(), e);
            throw new IllegalStateException("Unexpected error initializing actuator: " + e.getMessage(), e);
        }
    }

    /**
     * Unregisters an actuator.
     * 
     * @param actuatorName The name of the actuator to unregister
     */
    public void unregisterActuator(String actuatorName) {
        IActuator actuator = actuators.remove(actuatorName);
        if (actuator != null) {
            log.info("Unregistering actuator: {}", actuatorName);

            // Find context and shutdown
            for (IActuatorContext context : actuatorContexts) {
                if (context instanceof ActuatorContextImpl) {
                    ActuatorContextImpl ctx = (ActuatorContextImpl) context;
                    if (ctx.getMachineId().equals(engine.getMachineId())) {
                        try {
                            actuator.shutdown(context);
                        } catch (Exception e) {
                            log.error("Error shutting down actuator '{}': {}", actuatorName, e.getMessage(), e);
                        }
                        break;
                    }
                }
            }

            log.info("Actuator '{}' unregistered", actuatorName);
        }
    }

    /**
     * Shuts down all actuators.
     * Called when engine stops.
     */
    public void shutdownActuators() {
        log.info("Shutting down actuators for machine: {}", engine.getMachineId());

        for (Map.Entry<String, IActuator> entry : actuators.entrySet()) {
            String actuatorName = entry.getKey();
            IActuator actuator = entry.getValue();

            try {
                // Find matching context
                for (IActuatorContext context : actuatorContexts) {
                    if (context instanceof ActuatorContextImpl) {
                        ActuatorContextImpl ctx = (ActuatorContextImpl) context;
                        if (ctx.getMachineId().equals(engine.getMachineId())) {
                            actuator.shutdown(context);
                            break;
                        }
                    }
                }
                log.debug("Actuator '{}' shut down successfully", actuatorName);
            } catch (Exception e) {
                log.error("Error shutting down actuator '{}': {}", actuatorName, e.getMessage(), e);
            }
        }

        actuators.clear();
        actuatorContexts.clear();
        log.info("All actuators shut down for machine: {}", engine.getMachineId());
    }

    /**
     * Gets a registered actuator.
     * 
     * @param actuatorName The actuator name
     * @return The actuator, or null if not found
     */
    public IActuator getActuator(String actuatorName) {
        return actuators.get(actuatorName);
    }

    /**
     * Gets all registered actuator names.
     * 
     * @return List of actuator names
     */
    public List<String> getActuatorNames() {
        return new ArrayList<>(actuators.keySet());
    }
}
