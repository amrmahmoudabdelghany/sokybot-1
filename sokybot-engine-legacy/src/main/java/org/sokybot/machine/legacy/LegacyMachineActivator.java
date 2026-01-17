package org.sokybot.machine.legacy;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.sokybot.machine.config.MachineConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Bundle Activator for legacy machine package.
 * 
 * This activator starts the Spring Boot application context for the legacy
 * Spring State Machine-based machine package.
 * 
 * NOTE: This is legacy code. New engine architecture should use
 * org.sokybot.engine.core which is framework-agnostic.
 * 
 * @deprecated Legacy code - use org.sokybot.engine.core instead
 */
@Deprecated
public class LegacyMachineActivator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(LegacyMachineActivator.class);
    
    private static BundleContext bundleContext;
    private ConfigurableApplicationContext springContext;

    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Legacy Machine Bundle Starting...");
        bundleContext = context;
        
        // Use the current thread's classloader to avoid issues with Spring/OSGi
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            this.springContext = new SpringApplicationBuilder(MachineConfig.class)
                .web(WebApplicationType.NONE)
                .run();
            log.info("Legacy Machine Bundle: Spring context started.");
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Legacy Machine Bundle Stopping...");
        if (this.springContext != null) {
            this.springContext.close();
        }
        bundleContext = null;
        log.info("Legacy Machine Bundle Stopped.");
    }

    /**
     * Gets the OSGi BundleContext.
     * 
     * @return The BundleContext, or null if bundle is not started
     */
    public static BundleContext getBundleContext() {
        return bundleContext;
    }
}
