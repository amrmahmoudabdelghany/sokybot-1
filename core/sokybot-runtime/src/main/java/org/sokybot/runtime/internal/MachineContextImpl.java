package org.sokybot.runtime.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;

import org.sokybot.runtime.internal.domain.MachineInfo;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.IEngineFactory;
import org.sokybot.proxy.IConnectionListener;
import org.sokybot.proxy.IProxyConnection;
import org.sokybot.proxy.IProxyConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Implementation of IMachineContext that manages a single machine.
 * 
 * This implementation:
 * - Uses IEngineFactory to get engine (creates Spring context internally)
 * - Uses IProxyConnectionFactory to get proxy connection
 * - Provides access to packet publisher and page viewer via OSGi services
 * - No Spring dependencies - pure OSGi
 */
public class MachineContextImpl implements IMachineContext {

    private static final Logger log = LoggerFactory.getLogger(MachineContextImpl.class);

    private final MachineInfo machineInfo;
    private final IGroupContext groupContext;
    private final BundleContext bundleContext;

    private IEngine engine;
    private IProxyConnection proxyConnection;
    private org.sokybot.gamemodel.IGameModel gameModel;
    private java.util.Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> sharedTranslators;
    private org.sokybot.gameevents.ChunkedPacketManager chunkManager;
    private java.util.List<org.sokybot.network.IPacketSubscription> subscriptions = new java.util.ArrayList<>();

    public MachineContextImpl(MachineInfo machineInfo,
            IGroupContext groupContext,
            BundleContext bundleContext,
            IProxyConnection proxyConnection,
            org.sokybot.gamemodel.IGameModel gameModel,
            java.util.Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> sharedTranslators,
            org.sokybot.gameevents.ChunkedPacketManager chunkManager) {
        this.machineInfo = machineInfo;
        this.groupContext = groupContext;
        this.bundleContext = bundleContext;
        this.proxyConnection = proxyConnection;
        this.gameModel = gameModel;
        this.sharedTranslators = sharedTranslators;
        this.chunkManager = chunkManager;
        initializeMachineComponents();
    }

    private void initializeMachineComponents() {
        String machineId = fullName();

        // Set MDC context for logging (machine + group).
        MDC.put("sokybot.log.category", "MACHINE");
        MDC.put("sokybot.log.machineFullName", machineId);
        MDC.put("sokybot.log.machineName", name());
        MDC.put("sokybot.log.groupName", groupContext.name());

        log.info("Initializing machine context for: {}", machineId);

        try {
            // Get factories from OSGi service registry
            IEngineFactory engineFactory = getService(IEngineFactory.class);
            org.osgi.service.event.EventAdmin eventAdmin = getService(org.osgi.service.event.EventAdmin.class);

            if (engineFactory == null) {
                throw new IllegalStateException("IEngineFactory not available");
            }

            // 2. Create engine instance (engine factory creates Spring context internally)
            engine = engineFactory.createEngine(
                    machineId,
                    proxyConnection,
                    this.gameModel,
                    groupContext.name(), // Get group name from groupContext instead of machineInfo
                    machineInfo.getMachineName());

            // 3. Wire shared translators if available (per-game, shared across bots)
            // Each bot has its own ChunkedPacketManager (per-bot state)
            if (sharedTranslators != null && !sharedTranslators.isEmpty() && proxyConnection != null) {
                org.sokybot.network.IPacketPublisher publisher = proxyConnection.getPacketPublisher();
                if (publisher != null) {
                    sharedTranslators.forEach((opcode, translator) -> {
                        org.sokybot.network.IPacketSubscription sub = publisher.subscribe((packet) -> {
                            try {
                                // Translate packet - chunk manager accessed via registry
                                java.util.List<org.sokybot.gameevents.events.core.IGameEvent> events = translator
                                        .translate(machineId, packet, chunkManager);

                                if (events != null && eventAdmin != null) {
                                    events.forEach(event -> {
                                        java.util.Map<String, Object> props = new java.util.HashMap<>();
                                        props.put("event", event);
                                        // Enforce machine affiliation in event or properties?
                                        // Event usually carries it.
                                        // Post to EventAdmin
                                        // Topic convention: sokybot/game/<machineId>/<SimpleClassName>
                                        String topic = "sokybot/game/" + machineId + "/"
                                                + event.getClass().getSimpleName();
                                        eventAdmin.postEvent(new org.osgi.service.event.Event(topic, props));
                                    });
                                }
                            } catch (Exception e) {
                                log.error("Error translating packet opcode 0x{} for machine {}",
                                        Integer.toHexString(opcode).toUpperCase(), machineId, e);
                            }
                        }, opcode);
                        subscriptions.add(sub);
                    });
                    log.info("Wired {} shared translators for machine {} (using per-bot chunk manager)",
                            sharedTranslators.size(), machineId);
                }
            }

            log.info("Machine context initialized: {}", machineId);

        } catch (Exception e) {
            log.error("Failed to initialize machine components for: {}", machineId, e);
            throw new RuntimeException("Failed to initialize machine: " + machineId, e);
        } finally {
            // Clear MDC context for this thread.
            MDC.remove("sokybot.log.category");
            MDC.remove("sokybot.log.machineFullName");
            MDC.remove("sokybot.log.machineName");
            MDC.remove("sokybot.log.groupName");
        }
    }

    private static final int SERVICE_LOOKUP_RETRIES = 5;
    private static final long SERVICE_LOOKUP_DELAY_MS = 200;

    @Override
    public <T> T getService(Class<T> serviceClass) {
        if (bundleContext == null) {
            return null;
        }

        // Try multiple times with small delays to handle timing issues during startup
        for (int attempt = 0; attempt < SERVICE_LOOKUP_RETRIES; attempt++) {
            try {
                ServiceReference<T> ref = bundleContext.getServiceReference(serviceClass);
                if (ref != null) {
                    T service = bundleContext.getService(ref);
                    if (service != null) {
                        return service;
                    }
                }
            } catch (Exception e) {
                log.debug("Service {} not available (attempt {})", serviceClass.getName(), attempt + 1, e);
            }

            if (attempt < SERVICE_LOOKUP_RETRIES - 1) {
                try {
                    Thread.sleep(SERVICE_LOOKUP_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.warn("Service {} not available after {} retries", serviceClass.getName(), SERVICE_LOOKUP_RETRIES);
        return null;
    }

    @Override
    public String name() {
        return machineInfo.getMachineName();
    }

    @Override
    public String fullName() {
        return groupContext.name() + "." + name();
    }

    @Override
    public boolean isRunning() {
        return engine != null && engine.isRunning();
    }

    @Override
    public IEngine getEngine() {
        return this.engine;
    }

    @Override
    public String getMachineName() {
        return machineInfo.getMachineName();
    }

    @Override
    public String getGroupName() {
        return groupContext.name();
    }

    @Override
    public org.sokybot.proxy.IProxyConnection getProxyConnection() {
        return this.proxyConnection;
    }

    @Override
    public org.sokybot.gamemodel.IGameModel getGameModel() {
        return this.gameModel;
    }

    public void destroy() {
        MDC.put("sokybot.log.category", "MACHINE");
        MDC.put("sokybot.log.machineFullName", fullName());
        MDC.put("sokybot.log.machineName", name());
        MDC.put("sokybot.log.groupName", groupContext.name());

        log.info("Destroying machine context: {}", fullName());

        try {
            // Unregister chunk manager from registry
            if (chunkManager != null) {
                org.sokybot.gameevents.ChunkedPacketManagerRegistry.getInstance().unregister(fullName());
            }

            // Stop engine (destroys Spring context internally)
            if (engine != null) {
                IEngineFactory engineFactory = getService(IEngineFactory.class);
                if (engineFactory != null) {
                    engineFactory.destroyEngine(fullName());
                }
                engine = null;
            }

            // Properly destroy proxy connection via factory to avoid state leaks
            if (proxyConnection != null) {
                IProxyConnectionFactory proxyFactory = getService(IProxyConnectionFactory.class);
                if (proxyFactory != null) {
                    proxyFactory.destroyConnection(fullName());
                } else {
                    // Fallback to direct disconnect if factory is missing (should not happen)
                    proxyConnection.disconnect();
                }
                proxyConnection = null;
            }

            log.info("Machine context destroyed: {}", fullName());
        } catch (Exception e) {
            log.error("Error destroying machine context: {}", fullName(), e);
        } finally {
            MDC.remove("sokybot.log.category");
            MDC.remove("sokybot.log.machineFullName");
            MDC.remove("sokybot.log.machineName");
            MDC.remove("sokybot.log.groupName");
        }
    }
}
