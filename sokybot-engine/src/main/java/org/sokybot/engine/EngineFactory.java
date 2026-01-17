package org.sokybot.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.framework.BundleContext;
import org.sokybot.engine.core.EngineCore;
import org.sokybot.gamemodel.IGameModel;
import org.sokybot.gamemodel.IGameModelFactory;
import org.sokybot.proxy.IProxyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory implementation for creating engine instances.
 * 
 * Creates EngineCore instances for each machine.
 * Registers built-in actuators (connector, login, training).
 */
@Component(service = IEngineFactory.class)
public class EngineFactory implements IEngineFactory {
    
    private static final Logger log = LoggerFactory.getLogger(EngineFactory.class);
    
    private final Map<String, EngineCore> engines = new ConcurrentHashMap<>();

    private IGameModelFactory gameModelFactory;
    private BundleContext bundleContext;

    @Reference
    public void setGameModelFactory(IGameModelFactory gameModelFactory) {
        this.gameModelFactory = gameModelFactory;
    }

    @Activate
    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public IEngine createEngine(String machineId, IProxyConnection proxyConnection, 
                                String groupName, String machineName) {
        
        synchronized (engines) {
            if (engines.containsKey(machineId)) {
                throw new IllegalStateException("Engine already exists for machine: " + machineId);
            }
            
            log.info("Creating engine for machine: {}", machineId);
            
            try {
                // Create game model
                IGameModel gameModel = gameModelFactory.create(machineName);
                if (gameModel == null) {
                    throw new IllegalStateException("Failed to create game model for machine: " + machineName);
                }
                
                // Create engine core (no longer needs Settings or ISettingsManager)
                EngineCore engine = new EngineCore(
                    machineId, groupName, machineName,
                    proxyConnection, gameModel, bundleContext);
                
                engines.put(machineId, engine);
                
                // Actuators will be discovered via OSGi services when engine starts
                // No need to manually register them
                
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
            EngineCore engine = engines.remove(machineId);
            if (engine != null) {
                log.info("Destroying engine for machine: {}", machineId);
                engine.shutdown();
            } else {
                log.warn("Engine not found for machine: {}", machineId);
            }
        }
    }
    
    @Override
    public IEngine getEngine(String machineId) {
        return engines.get(machineId);
    }
}
