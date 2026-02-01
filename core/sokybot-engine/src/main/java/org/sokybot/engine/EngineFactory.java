package org.sokybot.engine;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.core.EngineCore;
import org.sokybot.gamemodel.IGameModel;

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

    // Injected actuators via OSGi Declarative Services
    private final List<IActuator> actuators = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindActuator(IActuator actuator) {
        log.info("Binding actuator services: {}", actuator.getName());
        this.actuators.add(actuator);
    }

    protected void unbindActuator(IActuator actuator) {
        log.info("Unbinding actuator services: {}", actuator.getName());
        this.actuators.remove(actuator);
    }

    @Override
    public IEngine createEngine(String machineId, IProxyConnection proxyConnection,
            IGameModel gameModel, String groupName, String machineName) {

        synchronized (engines) {
            if (engines.containsKey(machineId)) {
                throw new IllegalStateException("Engine already exists for machine: " + machineId);
            }

            log.info("Creating engine for machine: {}", machineId);

            try {
                if (gameModel == null) {
                    throw new IllegalArgumentException("Game model cannot be null");
                }

                // Create engine core, passing the injected actuators list
                EngineCore engine = new EngineCore(
                        machineId, groupName, machineName,
                        proxyConnection, gameModel, actuators);

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
