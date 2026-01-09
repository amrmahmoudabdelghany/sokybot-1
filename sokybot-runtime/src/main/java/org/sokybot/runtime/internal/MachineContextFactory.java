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
        return new MachineContextImpl(machineInfo, groupContext, bundleContext);
    }
    
    static void destroyMachineContext(IMachineContext machineContext) {
        if (machineContext instanceof MachineContextImpl) {
            ((MachineContextImpl) machineContext).destroy();
        }
    }
}
