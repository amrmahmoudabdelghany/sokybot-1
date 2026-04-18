package org.sokybot.runtime.internal;

import org.osgi.framework.BundleContext;
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
    private java.util.Map<Integer, java.util.List<org.sokybot.gameevents.events.core.IPacketTranslator>> sharedTranslators;
    private org.sokybot.gameevents.ChunkedPacketManager chunkManager;
    private final ISubscriptionScope subscriptionScope = new SubscriptionScopeImpl();
    private final TranslatorBridgeWirer translatorBridgeWirer;

    public MachineContextImpl(MachineInfo machineInfo,
            IGroupContext groupContext,
            BundleContext bundleContext,
            IProxyConnection proxyConnection,
            org.sokybot.gamemodel.IGameModel gameModel,
            org.sokybot.gamemodel.spi.IGameModelMutator gameModelMutator,
            java.util.Map<Integer, java.util.List<org.sokybot.gameevents.events.core.IPacketTranslator>> sharedTranslators,
            org.sokybot.gameevents.ChunkedPacketManager chunkManager) {
        this.machineInfo = machineInfo;
        this.groupContext = groupContext;
        this.bundleContext = bundleContext;
        this.proxyConnection = proxyConnection;
        this.gameModel = gameModel;
        this.sharedTranslators = sharedTranslators;
        this.chunkManager = chunkManager;
        this.translatorBridgeWirer = new TranslatorBridgeWirer(chunkManager, gameModelMutator, subscriptionScope);
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
            IEngineFactory engineFactory = RetryingServiceLocator.get(bundleContext, IEngineFactory.class, 15, 500L,
                    log);
            org.osgi.service.event.EventAdmin eventAdmin = RetryingServiceLocator.get(bundleContext,
                    org.osgi.service.event.EventAdmin.class, 15, 500L, log);
            org.sokybot.commons.event.IReactiveEventBus reactiveBus = RetryingServiceLocator.get(bundleContext,
                    org.sokybot.commons.event.IReactiveEventBus.class, 15, 500L, log);

            if (engineFactory == null) {
                throw new IllegalStateException("IEngineFactory not available");
            }

            // Wire translators BEFORE creating the engine: createEngine may call engine.start() (e.g. resume-on-boot),
            // which enables the login cycle immediately. If we subscribed after that, early packets (e.g. 0xA101) are
            // dropped and LoginState never receives the agent list.
            //
            // If the machine was installed before script translators or game data were ready, the constructor may
            // have captured an empty map — resolve from the group again here (with short retries).
            java.util.Map<Integer, java.util.List<org.sokybot.gameevents.events.core.IPacketTranslator>> translatorsToWire = sharedTranslators;
            if (translatorsToWire == null || translatorsToWire.isEmpty()) {
                if (groupContext instanceof ITranslatorRefreshable) {
                    translatorsToWire = TranslatorRetryHelper.resolveForMachineInit(
                            (ITranslatorRefreshable) groupContext,
                            translatorsToWire,
                            log,
                            machineId);
                }
            }
            if (proxyConnection != null) {
                org.sokybot.network.IPacketPublisher publisher = proxyConnection.getPacketPublisher();
                if (publisher != null) {
                    if (translatorsToWire != null && !translatorsToWire.isEmpty()) {
                        translatorBridgeWirer.wireTranslatorChains(publisher, translatorsToWire, machineId, eventAdmin,
                                reactiveBus);
                        log.info("Wired translator chains for machine {} (opcodes: {})",
                                machineId, Integer.valueOf(translatorsToWire.size()));
                    } else {
                        log.warn(
                                "No shared packet translators for machine {}",
                                machineId);
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

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return RetryingServiceLocator.get(bundleContext, serviceClass, 15, 500L, log);
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
