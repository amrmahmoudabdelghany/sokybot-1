package org.sokybot.engine.core;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.api.workflow.IWorkflowRegistry;
import org.sokybot.engine.core.workflow.WorkflowContextImpl;
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
    private final WorkflowContextImpl workflowContext;
    private final EngineCore engine;
    private final BundleContext bundleContext;
    
    private final Map<String, IActuator> actuators = new ConcurrentHashMap<>();
    private final List<IActuatorContext> actuatorContexts = new ArrayList<>();
    private final List<ServiceReference<IActuator>> serviceReferences = new ArrayList<>();
    
    public ActuatorRegistry(IWorkflowRegistry workflowRegistry,
                           WorkflowContextImpl workflowContext,
                           EngineCore engine,
                           BundleContext bundleContext) {
        this.workflowRegistry = workflowRegistry;
        this.workflowContext = workflowContext;
        this.engine = engine;
        this.bundleContext = bundleContext;
    }
    
    /**
     * Initializes actuators by discovering them via OSGi services.
     * Called when engine starts.
     */
    public void initializeActuators() {
        log.info("Initializing actuators for machine: {}", engine.getMachineId());
        
        if (bundleContext == null) {
            log.warn("No BundleContext available, cannot discover actuators via OSGi");
            return;
        }
        
        try {
            // Discover all IActuator services
            Collection<ServiceReference<IActuator>> references = bundleContext.getServiceReferences(
                IActuator.class, null);
            
            if (references != null && !references.isEmpty()) {
                for (ServiceReference<IActuator> ref : references) {
                    try {
                        IActuator actuator = bundleContext.getService(ref);
                        if (actuator != null) {
                            registerActuator(actuator);
                            serviceReferences.add(ref);
                        }
                    } catch (Exception e) {
                        log.error("Failed to initialize actuator from service reference: {}", e.getMessage(), e);
                    }
                }
                log.info("Discovered and processed {} actuators via OSGi", references.size());
            } else {
                log.warn("No actuators discovered via OSGi services");
            }
        } catch (Exception e) {
            log.error("Failed to query actuator services: {}", e.getMessage(), e);
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
            engine.getGroupName(), engine.getMachineName());
        
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
        
        // Unget OSGi services
        if (bundleContext != null) {
            for (ServiceReference<IActuator> ref : serviceReferences) {
                try {
                    bundleContext.ungetService(ref);
                } catch (Exception e) {
                    log.warn("Failed to unget service reference: {}", e.getMessage());
                }
            }
        }
        
        actuators.clear();
        actuatorContexts.clear();
        serviceReferences.clear();
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
