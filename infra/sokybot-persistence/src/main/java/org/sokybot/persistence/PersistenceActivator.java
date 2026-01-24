package org.sokybot.persistence;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OSGi Bundle Activator for Sokybot Persistence Bundle.
 * 
 * Note: With the new architecture, most services are registered via OSGi DS annotations.
 * This activator is kept for compatibility and potential future initialization logic.
 */
public class PersistenceActivator implements BundleActivator {
    
    private static final Logger logger = LoggerFactory.getLogger(PersistenceActivator.class);
    
    @Override
    public void start(BundleContext context) throws Exception {
        logger.info("Starting Sokybot Persistence Bundle");
        // Services are registered via OSGi DS annotations:
        // - IPersistenceContextManager (PersistenceContextManagerImpl)
        // - IGamePersistenceFactory (GamePersistenceFactoryImpl)
        // - All repository implementations
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        logger.info("Stopping Sokybot Persistence Bundle");
        // OSGi DS will handle cleanup of services
    }
}
