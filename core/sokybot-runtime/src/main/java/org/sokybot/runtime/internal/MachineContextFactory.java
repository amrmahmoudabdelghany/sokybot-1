package org.sokybot.runtime.internal;

import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.internal.domain.MachineInfo;

/**
 * Internal factory for creating IMachineContext instances.
 * Not exposed as OSGi service - used internally by GroupContextImpl.
 */
class MachineContextFactory {

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

        org.sokybot.proxy.IProxyConnectionFactory proxyFactory = getService(bundleContext,
                org.sokybot.proxy.IProxyConnectionFactory.class);
        if (proxyFactory == null) {
            throw new IllegalStateException("IProxyConnectionFactory not available");
        }

        org.sokybot.gamemodel.IGameModelFactory gameModelFactory = getService(bundleContext,
                org.sokybot.gamemodel.IGameModelFactory.class);
        if (gameModelFactory == null) {
            throw new IllegalStateException("IGameModelFactory not available");
        }

        String machineId = groupContext.name() + "." + machineInfo.getMachineName();
        // Create Proxy Connection
        org.sokybot.proxy.IProxyConnection connection = proxyFactory.createConnection(machineId);

        // Create Game Model (full id group.machine so events match IGameEvent#getFullName())
        org.sokybot.gamemodel.IGameModel gameModel = gameModelFactory.create(machineId);

        // Get shared translators from GroupContext (per-game, memory optimized)
        // Access package-private method since MachineContextFactory is in same package
        GroupContextImpl groupContextImpl = (GroupContextImpl) groupContext;
        java.util.Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> sharedTranslators = java.util.Map
                .of();
        for (int tAttempt = 0; tAttempt < 6; tAttempt++) {
            if (tAttempt > 0) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                groupContextImpl.invalidateSharedTranslators();
            }
            sharedTranslators = groupContextImpl.getTranslators();
            if (!sharedTranslators.isEmpty()) {
                break;
            }
        }

        // Create per-bot ChunkedPacketManager (stateful, must be per-bot)
        org.sokybot.gameevents.ChunkedPacketManager chunkManager = new org.sokybot.gameevents.ChunkedPacketManager();

        // Register chunk manager in registry so translators can access it
        org.sokybot.gameevents.ChunkedPacketManagerRegistry.getInstance().register(machineId, chunkManager);

        return new MachineContextImpl(machineInfo, groupContext, bundleContext, connection, gameModel,
                sharedTranslators, chunkManager);
    }

    private static final int SERVICE_LOOKUP_RETRIES = 5;
    private static final long SERVICE_LOOKUP_DELAY_MS = 200;

    private static <T> T getService(org.osgi.framework.BundleContext context, Class<T> clazz) {
        if (context == null)
            return null;

        // Try multiple times with small delays to handle timing issues during startup
        for (int attempt = 0; attempt < SERVICE_LOOKUP_RETRIES; attempt++) {
            org.osgi.framework.ServiceReference<T> ref = context.getServiceReference(clazz);
            if (ref != null) {
                T service = context.getService(ref);
                if (service != null) {
                    return service;
                }
            }

            if (attempt < SERVICE_LOOKUP_RETRIES - 1) {
                // Wait before retry
                try {
                    Thread.sleep(SERVICE_LOOKUP_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return null;
    }

    static void destroyMachineContext(IMachineContext machineContext) {
        if (machineContext instanceof MachineContextImpl) {
            ((MachineContextImpl) machineContext).destroy();
        }
    }
}
