package org.sokybot.engine.internal;

import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.sokybot.engine.IEngine;
import org.sokybot.engine.IEngineFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.event.IReactiveEventBus;
import org.sokybot.commons.osgi.ServiceHandle;
import org.sokybot.engine.api.EngineEvent;
import org.sokybot.engine.api.extension.IActuator;
import org.sokybot.engine.api.handler.IEngineEventHandler;
import org.sokybot.engine.api.handler.IEngineEventMediator;
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
@Component(service = IEngineFactory.class, immediate = true)
public class EngineFactory implements IEngineFactory {

    private static final Logger log = LoggerFactory.getLogger(EngineFactory.class);
    private static final String BUILD_SIGNATURE = "EngineFactory-2026-03-27-R2";

    private final Map<String, EngineCore> engines = new ConcurrentHashMap<>();

    // Injected actuators via OSGi Declarative Services
    private final List<IActuator> actuators = new CopyOnWriteArrayList<>();
    private final List<IEngineEventHandler<? extends EngineEvent>> handlers = new CopyOnWriteArrayList<>();
    private final ServiceHandle<IEngineEventMediator> eventMediator = ServiceHandle.create();
    private final ServiceHandle<IReactiveEventBus> reactiveEventBus = ServiceHandle.create();

    private BundleContext bundleContext;
    private boolean resumeOnBoot;
    private Set<String> resumeMachines;

    @Activate
    protected void activate(BundleContext context) {
        this.bundleContext = context;
        this.resumeOnBoot = Boolean.parseBoolean(System.getProperty("sokybot.engine.resumeOnBoot", "false"));
        String rawResumeMachines = System.getProperty("sokybot.engine.resumeMachines", "").trim();
        this.resumeMachines = Arrays.stream(rawResumeMachines.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        log.info("Engine Factory activated [{}] (resumeOnBoot={}, resumeMachines={})",
                BUILD_SIGNATURE, resumeOnBoot, resumeMachines.size());
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindActuator(IActuator actuator) {
        log.info("Binding actuator service: {}", actuator.getName());
        this.actuators.add(actuator);

        // Register to all existing engines
        engines.values().forEach(engine -> {
            try {
                engine.getActuatorRegistry().registerActuator(actuator);
            } catch (Exception e) {
                log.error("Failed to register actuator {} to engine {}", actuator.getName(), engine.getMachineId(),
                        e);
            }
        });
    }

    protected void unbindActuator(IActuator actuator) {
        log.info("Unbinding actuator service: {}", actuator.getName());
        this.actuators.remove(actuator);

        // Unregister from all existing engines
        engines.values().forEach(engine -> {
            try {
                engine.getActuatorRegistry().unregisterActuator(actuator.getName());
            } catch (Exception e) {
                log.error("Error unregistering actuator {} from engine {}", actuator.getName(), engine.getMachineId(),
                        e);
            }
        });
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindEventHandler(IEngineEventHandler<? extends EngineEvent> handler) {
        this.handlers.add(handler);
        engines.values().forEach(engine -> engine.bindEventHandler(handler));
    }

    protected void unbindEventHandler(IEngineEventHandler<? extends EngineEvent> handler) {
        this.handlers.remove(handler);
        engines.values().forEach(engine -> engine.unbindEventHandler(handler));
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetEventMediator")
    protected void setEventMediator(IEngineEventMediator mediator) {
        this.eventMediator.bind(mediator);
        engines.values().forEach(engine -> engine.setEventMediator(mediator));
    }

    protected void unsetEventMediator(IEngineEventMediator mediator) {
        this.eventMediator.unbind(mediator);
        engines.values().forEach(engine -> engine.clearEventMediator(mediator));
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, unbind = "unsetReactiveEventBus")
    protected void setReactiveEventBus(IReactiveEventBus bus) {
        this.reactiveEventBus.bind(bus);
    }

    protected void unsetReactiveEventBus(IReactiveEventBus bus) {
        this.reactiveEventBus.unbind(bus);
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
                        proxyConnection, gameModel, actuators, handlers,
                        eventMediator.tryGet().orElse(null), bundleContext,
                        reactiveEventBus.tryGet().orElse(null));

                engines.put(machineId, engine);

                log.info("Engine created successfully for machine: {}", machineId);
                if (shouldResumeOnBoot(machineId, groupName, machineName)) {
                    log.info("Resuming engine on boot for machine: {} (idle until machine.start / CONNECT)", machineId);
                    engine.start();
                } else {
                    log.info("Engine created in STOPPED state for machine: {}", machineId);
                }

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

    private boolean shouldResumeOnBoot(String machineId, String groupName, String machineName) {
        if (!resumeOnBoot) {
            return false;
        }
        if (resumeMachines.isEmpty()) {
            return true;
        }
        String groupMachine = groupName + "." + machineName;
        return resumeMachines.contains(machineId)
                || resumeMachines.contains(groupMachine)
                || resumeMachines.contains(machineName);
    }
}
