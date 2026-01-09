package org.sokybot.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.sokybot.app.AppConstants;
import org.sokybot.machine.MachineConfig;
import org.sokybot.proxy.IProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Factory implementation for creating engine instances.
 * 
 * Creates Spring contexts with MachineConfig for each machine.
 * Each engine instance wraps a Spring application context.
 */
@Component(service = IEngineFactory.class)
public class EngineFactory implements IEngineFactory {
    
    private static final Logger log = LoggerFactory.getLogger(EngineFactory.class);
    
    private final Map<String, EngineImpl> engines = new ConcurrentHashMap<>();
    
    @Override
    public IEngine createEngine(String machineId, IProxyConnection proxyConnection, 
                                String groupName, String machineName) {
        
        synchronized (engines) {
            if (engines.containsKey(machineId)) {
                throw new IllegalStateException("Engine already exists for machine: " + machineId);
            }
            
            log.info("Creating engine for machine: {}", machineId);
            
            try {
                // Create Spring context with MachineConfig
                // Register proxy connection and related beans before context starts
                ConfigurableApplicationContext springContext = new SpringApplicationBuilder(MachineConfig.class)
                        .properties(Map.of(
                                AppConstants.MACHINE_NAME, machineName,
                                AppConstants.GROUP_NAME, groupName,
                                "spring.config.location", "classpath:machine.properties"))
                        .initializers((ctx) -> {
                            // Register machine info
                            ctx.getBeanFactory().registerSingleton("machineId", machineId);
                            ctx.getBeanFactory().registerSingleton("groupName", groupName);
                            ctx.getBeanFactory().registerSingleton("machineName", machineName);
                            
                            // Register proxy connection as singleton bean
                            // This will be used by EngineConfig.packetPublisher() bean
                            ctx.getBeanFactory().registerSingleton("proxyConnection", proxyConnection);
                            
                            // Note: EngineConfig.proxyConnection() bean method will be skipped
                            // since we've already registered a bean with that name.
                            // ConnectionHandler (IConnectionListener) will be created by Spring
                            // and should work with the registered proxy connection.
                        })
                        .run();
                
                // Wire ConnectionHandler to proxy connection if it exists
                try {
                    org.sokybot.proxy.IConnectionListener connectionHandler = 
                        springContext.getBean(org.sokybot.proxy.IConnectionListener.class);
                    if (connectionHandler != null && proxyConnection != null) {
                        proxyConnection.setConnectionListener(connectionHandler);
                        log.debug("Wired ConnectionHandler to proxy connection for machine: {}", machineId);
                    }
                } catch (Exception e) {
                    log.warn("Could not wire ConnectionHandler to proxy connection (may not exist): {}", e.getMessage());
                }
                
                // Wrap Spring context as IEngine
                EngineImpl engine = new EngineImpl(machineId, springContext);
                engines.put(machineId, engine);
                
                log.info("Engine created successfully for machine: {}", machineId);
                return engine;
                
            } catch (Exception e) {
                log.error("Failed to create engine for machine: {}", machineId, e);
                throw new RuntimeException("Failed to create engine for machine: " + machineId, e);
            }
        }
    }
    
    @Override
    public void destroyEngine(String machineId) {
        synchronized (engines) {
            EngineImpl engine = engines.remove(machineId);
            if (engine != null) {
                log.info("Destroying engine for machine: {}", machineId);
                engine.stop();
            } else {
                log.warn("Engine not found for machine: {}", machineId);
            }
        }
    }
    
    @Override
    public IEngine getEngine(String machineId) {
        return engines.get(machineId);
    }
    
    /**
     * Internal implementation of IEngine that wraps a Spring context.
     */
    private static class EngineImpl implements IEngine {
        
        private final String machineId;
        private final ConfigurableApplicationContext springContext;
        private volatile boolean running = false;
        
        public EngineImpl(String machineId, ConfigurableApplicationContext springContext) {
            this.machineId = machineId;
            this.springContext = springContext;
        }
        
        @Override
        public String getMachineId() {
            return machineId;
        }
        
        @Override
        public void start() {
            if (!running) {
                synchronized (this) {
                    if (!running) {
                        // Spring context is already started when created
                        // But we can explicitly start it if needed
                        if (!springContext.isRunning()) {
                            springContext.start();
                        }
                        running = true;
                        log.debug("Engine started for machine: {}", machineId);
                    }
                }
            }
        }
        
        @Override
        public void stop() {
            if (running) {
                synchronized (this) {
                    if (running) {
                        try {
                            springContext.stop();
                            springContext.close();
                            running = false;
                            log.debug("Engine stopped for machine: {}", machineId);
                        } catch (Exception e) {
                            log.error("Error stopping engine for machine: {}", machineId, e);
                            running = false;
                        }
                    }
                }
            }
        }
        
        @Override
        public boolean isRunning() {
            return running && springContext.isRunning();
        }
    }
}
