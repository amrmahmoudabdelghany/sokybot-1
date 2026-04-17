package org.sokybot.runtime.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.sokybot.gameevents.GatewayAgentListTranslator;
import org.sokybot.gameevents.GatewayLoginResponseTranslator;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.commons.lifecycle.ISubscriptionScope;
import org.sokybot.commons.lifecycle.SubscriptionScopeImpl;

import org.sokybot.runtime.internal.domain.MachineInfo;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.IEngineFactory;
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

    private volatile IEngine engine;
    private volatile boolean engineInitialized;
    private IProxyConnection proxyConnection;
    private org.sokybot.gamemodel.IGameModel gameModel;
    private org.sokybot.gamemodel.spi.IGameModelMutator gameModelMutator;
    private java.util.Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> sharedTranslators;
    private org.sokybot.gameevents.ChunkedPacketManager chunkManager;
    private final ISubscriptionScope subscriptionScope = new SubscriptionScopeImpl();

    public MachineContextImpl(MachineInfo machineInfo,
            IGroupContext groupContext,
            BundleContext bundleContext,
            IProxyConnection proxyConnection,
            org.sokybot.gamemodel.IGameModel gameModel,
            org.sokybot.gamemodel.spi.IGameModelMutator gameModelMutator,
            java.util.Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> sharedTranslators,
            org.sokybot.gameevents.ChunkedPacketManager chunkManager) {
        this.machineInfo = machineInfo;
        this.groupContext = groupContext;
        this.bundleContext = bundleContext;
        this.proxyConnection = proxyConnection;
        this.gameModel = gameModel;
        this.gameModelMutator = gameModelMutator;
        this.sharedTranslators = sharedTranslators;
        this.chunkManager = chunkManager;
        log.info("Machine context created (engine will initialize lazily): {}", fullName());
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
            org.sokybot.commons.event.IReactiveEventBus reactiveBus = getService(
                    org.sokybot.commons.event.IReactiveEventBus.class);

            if (engineFactory == null) {
                throw new IllegalStateException("IEngineFactory not available");
            }

            // Wire translators BEFORE creating the engine: createEngine may call engine.start() (e.g. resume-on-boot),
            // which enables the login cycle immediately. If we subscribed after that, early packets (e.g. 0xA101) are
            // dropped and LoginState never receives the agent list.
            //
            // If the machine was installed before script translators or game data were ready, the constructor may
            // have captured an empty map — resolve from the group again here (with short retries).
            java.util.Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> translatorsToWire = sharedTranslators;
            if (translatorsToWire == null || translatorsToWire.isEmpty()) {
                if (groupContext instanceof GroupContextImpl) {
                    GroupContextImpl g = (GroupContextImpl) groupContext;
                    for (int tAttempt = 0; tAttempt < 12; tAttempt++) {
                        if (tAttempt > 0) {
                            try {
                                Thread.sleep(400);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                            g.invalidateSharedTranslators();
                        }
                        translatorsToWire = g.getTranslators();
                        if (translatorsToWire != null && !translatorsToWire.isEmpty()) {
                            log.info("Resolved {} packet translators for machine {} after deferred load (attempt {})",
                                    translatorsToWire.size(), machineId, tAttempt + 1);
                            break;
                        }
                    }
                }
            }
            if (proxyConnection != null) {
                org.sokybot.network.IPacketPublisher publisher = proxyConnection.getPacketPublisher();
                if (publisher != null) {
                    if (translatorsToWire != null && !translatorsToWire.isEmpty()) {
                        translatorsToWire.forEach(
                                (opcode, translator) -> wireTranslatorBridge(publisher, opcode, translator, machineId,
                                        eventAdmin, reactiveBus));
                        log.info("Wired {} shared translators for machine {} (using per-bot chunk manager)",
                                translatorsToWire.size(), machineId);
                    } else {
                        log.warn(
                                "No shared packet translators for machine {} — built-in 0xA101/0xA102 bridges still apply where wired",
                                machineId);
                    }
                    // Second subscriber for 0xA101 when the shared map uses another translator (e.g. script):
                    // script may return no events while this parser still succeeds. If the map already exposes
                    // GatewayAgentListTranslator.INSTANCE, one subscription is enough.
                    if (translatorsToWire == null || translatorsToWire.get(0xA101) != GatewayAgentListTranslator.INSTANCE) {
                        wireTranslatorBridge(publisher, 0xA101, GatewayAgentListTranslator.INSTANCE, machineId,
                                eventAdmin, reactiveBus);
                        log.info("Wired built-in gateway agent-list translator (0xA101) for machine {}", machineId);
                    }
                    if (translatorsToWire == null || translatorsToWire.get(0xA102) != GatewayLoginResponseTranslator.INSTANCE) {
                        wireTranslatorBridge(publisher, 0xA102, GatewayLoginResponseTranslator.INSTANCE, machineId,
                                eventAdmin, reactiveBus);
                        log.info("Wired built-in gateway login response translator (0xA102) for machine {}", machineId);
                    }
                }
            }

            engine = engineFactory.createEngine(
                    machineId,
                    proxyConnection,
                    this.gameModel,
                    groupContext.name(),
                    machineInfo.getMachineName());

            log.info("Machine context initialized: {}", machineId);

        } catch (Exception e) {
            log.error("Failed to initialize machine components for: {}", machineId, e);
        } finally {
            // Clear MDC context for this thread.
            MDC.remove("sokybot.log.category");
            MDC.remove("sokybot.log.machineFullName");
            MDC.remove("sokybot.log.machineName");
            MDC.remove("sokybot.log.groupName");
        }
    }

    private void wireTranslatorBridge(org.sokybot.network.IPacketPublisher publisher, Integer opcode,
            org.sokybot.gameevents.events.core.IPacketTranslator translator, String machineId,
            org.osgi.service.event.EventAdmin eventAdmin,
            org.sokybot.commons.event.IReactiveEventBus reactiveBus) {
        int op = opcode == null ? translator.getOpcode() : opcode.intValue();
        org.sokybot.network.IPacketSubscription sub = publisher.subscribe((packet) -> {
            try {
                java.util.List<org.sokybot.gameevents.events.core.IGameEvent> events = translator.translate(machineId,
                        packet, chunkManager);
                if (log.isDebugEnabled() && op == 0xA101) {
                    int eventCount = events != null ? events.size() : 0;
                    log.debug("Translator bridge machine={} opcode=0xA101 translator={} produced {} events", machineId,
                            translator.getClass().getName(), eventCount);
                }
                if (op == 0xA101 && (events == null || events.isEmpty())) {
                    if (translator == GatewayAgentListTranslator.INSTANCE) {
                        log.debug(
                                "Built-in 0xA101 translator produced no events for machine {} (size={}); capture payload hex if this persists",
                                machineId, packet != null ? packet.getPacketSize() : -1);
                    } else {
                        log.warn(
                                "Agent list packet (0xA101) produced no events for machine {} (size={}) via {}; built-in bridge may still parse",
                                machineId, packet != null ? packet.getPacketSize() : -1,
                                translator.getClass().getName());
                    }
                }
                if (events != null) {
                    for (org.sokybot.gameevents.events.core.IGameEvent event : events) {
                        if (event == null) {
                            continue;
                        }
                        gameModelMutator.dispatchGameEvent(event);
                        java.util.Map<String, Object> props = new java.util.HashMap<>();
                        props.put("event", event);
                        props.put("machineId", machineId);
                        props.put("fullName", machineId);
                        String topic = org.sokybot.commons.osgi.OsgiEventTopics.gameTopic(machineId,
                                event.getClass().getSimpleName());
                        if (log.isDebugEnabled()) {
                            log.debug("Posting game event machine={} topic={} type={}", machineId, topic,
                                    event.getClass().getName());
                        }
                        if (eventAdmin != null) {
                            eventAdmin.postEvent(new org.osgi.service.event.Event(topic, props));
                        }
                        if (reactiveBus != null) {
                            reactiveBus.publish(event);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error translating packet opcode 0x{} for machine {}",
                        Integer.toHexString(op).toUpperCase(), machineId, e);
            }
        }, op);
        subscriptionScope.register(sub);
    }

    private static final int SERVICE_LOOKUP_RETRIES = 15;
    private static final long SERVICE_LOOKUP_DELAY_MS = 500;

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
        ensureEngineInitialized();
        return this.engine;
    }

    private synchronized void ensureEngineInitialized() {
        if (engineInitialized) {
            return;
        }
        engineInitialized = true;
        initializeMachineComponents();
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

    @Override
    public ISubscriptionScope getSubscriptionScope() {
        return subscriptionScope;
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

            subscriptionScope.close();

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
