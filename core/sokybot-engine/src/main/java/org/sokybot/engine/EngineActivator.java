package org.sokybot.engine;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Bundle Activator for sokybot-engine.
 * 
 * The new engine architecture is framework-agnostic and does not require
 * Spring Boot. This activator only stores the BundleContext for OSGi service
 * discovery used by the new EngineCore and actuator system.
 * 
 * Legacy Spring Boot configuration has been removed. The new engine uses
 * OSGi service registration directly via @Component annotations.
 */
public class EngineActivator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(EngineActivator.class);
    
    private static BundleContext bundleContext;

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Sokybot Engine Bundle Starting...");
        bundleContext = context;
        log.info("Sokybot Engine Bundle Started. BundleContext available for OSGi service discovery.");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Sokybot Engine Bundle Stopping...");
        bundleContext = null;
        log.info("Sokybot Engine Bundle Stopped.");
    }

    /**
     * Gets the OSGi BundleContext for service discovery.
     * Used by EngineFactory to pass to EngineCore for actuator discovery.
     * 
     * @return The BundleContext, or null if bundle is not started
     */
    public static BundleContext getBundleContext() {
        return bundleContext;
    }
}
