package org.sokybot.runtime.internal;

import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.app.domain.MachineInfo;

/**
 * Internal factory for creating IMachineContext instances.
 * Not exposed as OSGi service - used internally by GroupContextImpl.
 */
class MachineContextFactory {
    
    /**
     * Creates a new machine context by assembling components from OSGi services.
     * 
     * @param machineInfo The machine information
     * @param groupContext The parent group context
     * @param bundleContext OSGi bundle context (for service lookup)
     * @return The created machine context
     */
    static IMachineContext createMachineContext(MachineInfo machineInfo,
                                                 IGroupContext groupContext,
                                                 org.osgi.framework.BundleContext bundleContext) {
        
        org.sokybot.proxy.IProxyConnectionFactory proxyFactory = getService(bundleContext, org.sokybot.proxy.IProxyConnectionFactory.class);
        if (proxyFactory == null) {
            throw new IllegalStateException("IProxyConnectionFactory not available");
        }
        
        org.sokybot.gamemodel.IGameModelFactory gameModelFactory = getService(bundleContext, org.sokybot.gamemodel.IGameModelFactory.class);
        if (gameModelFactory == null) {
            throw new IllegalStateException("IGameModelFactory not available");
        }
        
        String machineId = groupContext.name() + "." + machineInfo.getMachineName();
        // Create Proxy Connection
        org.sokybot.proxy.IProxyConnection connection = proxyFactory.createConnection(machineId);
        
        // Create Game Model (using machine name for unique model per bot)
        org.sokybot.gamemodel.IGameModel gameModel = gameModelFactory.create(machineInfo.getMachineName());
        
        // Get shared translators from GroupContext (per-game, memory optimized)
        java.util.Map<Integer, org.sokybot.gameevents.events.core.IPacketTranslator> sharedTranslators = groupContext.getTranslators();
        
        // Create per-bot ChunkedPacketManager (stateful, must be per-bot)
        org.sokybot.gameevents.ChunkedPacketManager chunkManager = new org.sokybot.gameevents.ChunkedPacketManager();
        
        // Register chunk manager in registry so translators can access it
        org.sokybot.gameevents.ChunkedPacketManagerRegistry.getInstance().register(machineId, chunkManager);
        
        return new MachineContextImpl(machineInfo, groupContext, bundleContext, connection, gameModel, sharedTranslators, chunkManager);
    }
    
    private static <T> T getService(org.osgi.framework.BundleContext context, Class<T> clazz) {
        if (context == null) return null;
        org.osgi.framework.ServiceReference<T> ref = context.getServiceReference(clazz);
        if (ref != null) return context.getService(ref);
        return null;
    }
    
    static void destroyMachineContext(IMachineContext machineContext) {
        if (machineContext instanceof MachineContextImpl) {
            ((MachineContextImpl) machineContext).destroy();
        }
    }
}
