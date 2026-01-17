package org.sokybot.engine.test.util;

import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.extension.IActuatorContext;
import org.sokybot.engine.api.extension.BundleException;
import org.sokybot.engine.api.workflow.ICycleDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock actuator for testing.
 * Tracks initialization/shutdown calls and can register cycles.
 */
public class MockActuator implements IActuator {
    
    private final String name;
    private boolean initialized = false;
    private boolean shutdown = false;
    private IActuatorContext context;
    private BundleException initializationException = null;
    private final List<ICycleDefinition> registeredCycles = new ArrayList<>();
    private Runnable onInitialize;
    private Runnable onShutdown;
    
    /**
     * Creates a new MockActuator with the given name.
     */
    public MockActuator(String name) {
        this.name = name;
    }
    
    /**
     * Sets an exception to throw during initialization.
     */
    public MockActuator withInitializationException(BundleException exception) {
        this.initializationException = exception;
        return this;
    }
    
    /**
     * Sets a callback to run during initialization.
     */
    public MockActuator withOnInitialize(Runnable callback) {
        this.onInitialize = callback;
        return this;
    }
    
    /**
     * Sets a callback to run during shutdown.
     */
    public MockActuator withOnShutdown(Runnable callback) {
        this.onShutdown = callback;
        return this;
    }
    
    /**
     * Adds a cycle to register during initialization.
     */
    public MockActuator withCycle(ICycleDefinition cycle) {
        this.registeredCycles.add(cycle);
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void initialize(IActuatorContext context) throws BundleException {
        if (initializationException != null) {
            throw initializationException;
        }
        this.initialized = true;
        this.context = context;
        
        // Register cycles
        for (ICycleDefinition cycle : registeredCycles) {
            context.getWorkflowRegistry().registerCycle(cycle);
        }
        
        if (onInitialize != null) {
            onInitialize.run();
        }
    }
    
    @Override
    public void shutdown(IActuatorContext context) {
        this.shutdown = true;
        if (onShutdown != null) {
            onShutdown.run();
        }
    }
    
    /**
     * Checks if the actuator was initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Checks if the actuator was shut down.
     */
    public boolean isShutdown() {
        return shutdown;
    }
    
    /**
     * Gets the context passed during initialization.
     */
    public IActuatorContext getContext() {
        return context;
    }
    
    /**
     * Resets the mock actuator state.
     */
    public void reset() {
        this.initialized = false;
        this.shutdown = false;
        this.context = null;
        this.registeredCycles.clear();
    }
}
