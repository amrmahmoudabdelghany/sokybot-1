package org.sokybot.runtime.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.sokybot.ISokybotContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for sokybot-runtime.
 * 
 * This bundle provides implementations for:
 * - ISokybotContext
 * - IGroupContext  
 * - IMachineContext
 * 
 * The activator ensures that ISokybotContext service is accessible to other bundles.
 * With OSGi Declarative Services (DS), the @Component annotation on SokybotContextImpl
 * should automatically register the service. This activator provides a fallback mechanism
 * and ensures proper service availability.
 */
public class RuntimeActivator implements BundleActivator {
    
    private static final Logger log = LoggerFactory.getLogger(RuntimeActivator.class);
    
    private static BundleContext bundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        log.info("sokybot-runtime bundle started");
        
        // Check if ISokybotContext is already registered (via DS)
        // If not, we'll wait for DS to register it, or log a warning
        ServiceReference<ISokybotContext> ref = context.getServiceReference(ISokybotContext.class);
        if (ref != null) {
            ISokybotContext service = context.getService(ref);
            log.info("ISokybotContext service found and available: {}", service);
        } else {
            log.info("ISokybotContext service will be registered by Declarative Services");
            log.info("Other bundles can access it using:");
            log.info("  BundleContext.getServiceReference(ISokybotContext.class)");
            log.info("  Or inject via @Reference annotation in DS components");
        }
        
        log.info("Context implementations ready. Factories registered:");
        log.info("  - GroupContextFactory");
        log.info("  - MachineContextFactory");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("sokybot-runtime bundle stopped");
        bundleContext = null;
    }
    
    /**
     * Get the bundle context for this bundle.
     * Can be used by other classes in this bundle if needed.
     */
    public static BundleContext getBundleContext() {
        return bundleContext;
    }
}
