package org.sokybot.runtime.internal;

import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.internal.domain.MachineInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal factory for creating IMachineContext instances.
 * Not exposed as OSGi service - used internally by GroupContextImpl.
 */
class MachineContextFactory {

    private static final Logger log = LoggerFactory.getLogger(MachineContextFactory.class);

    /**
     * Creates a new machine context by assembling components from OSGi services.
     * 
     * @param machineInfo   The machine information (runtime domain)
     * @param groupContext  The parent group context
     * @param bundleContext OSGi bundle context (for service lookup)
     * @return The created machine context
     */
    static IMachineContext createMachineContext(MachineInfo machineInfo,
            IGroupContext groupContext,
            org.osgi.framework.BundleContext bundleContext) {

        org.sokybot.proxy.IProxyConnectionFactory proxyFactory = RetryingServiceLocator.get(bundleContext,
                org.sokybot.proxy.IProxyConnectionFactory.class, 5, 200L, log);
        if (proxyFactory == null) {
            throw new IllegalStateException("IProxyConnectionFactory not available");
        }

        org.sokybot.gamemodel.IGameModelFactory gameModelFactory = RetryingServiceLocator.get(bundleContext,
                org.sokybot.gamemodel.IGameModelFactory.class, 5, 200L, log);
        if (gameModelFactory == null) {
            throw new IllegalStateException("IGameModelFactory not available");
        }

        String machineId = groupContext.name() + "." + machineInfo.getMachineName();
        // Create Proxy Connection
        org.sokybot.proxy.IProxyConnection connection = proxyFactory.createConnection(machineId);

        // Create Game Model (full id group.machine so events match IGameEvent#getFullName())
        org.sokybot.gamemodel.IGameModel gameModel = gameModelFactory.create(machineId);
        org.sokybot.gamemodel.spi.IGameModelMutator gameModelMutator = gameModelFactory.getMutator(gameModel);

        if (!(groupContext instanceof ITranslatorRefreshable)) {
            throw new IllegalStateException("Group context must support ITranslatorRefreshable");
        }
        ITranslatorRefreshable translatorRefreshable = (ITranslatorRefreshable) groupContext;
        java.util.Map<Integer, java.util.List<org.sokybot.gameevents.events.core.IPacketTranslator>> sharedTranslators = TranslatorRetryHelper
                .resolveForFactory(translatorRefreshable);

        // Create per-bot ChunkedPacketManager (stateful, must be per-bot)
        org.sokybot.gameevents.ChunkedPacketManager chunkManager = new org.sokybot.gameevents.ChunkedPacketManager();

        // Register chunk manager in registry so translators can access it
        org.sokybot.gameevents.ChunkedPacketManagerRegistry.getInstance().register(machineId, chunkManager);

        return new MachineContextImpl(machineInfo, groupContext, bundleContext, connection, gameModel, gameModelMutator,
                sharedTranslators, chunkManager);
    }

    static void destroyMachineContext(IMachineContext machineContext) {
        if (machineContext instanceof MachineContextImpl) {
            ((MachineContextImpl) machineContext).destroy();
        }
    }
}
