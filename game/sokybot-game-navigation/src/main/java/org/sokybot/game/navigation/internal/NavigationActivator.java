package org.sokybot.game.navigation.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.sokybot.game.navigation.IRuteFinderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator for sokybot-game-navigation.
 * Registers the IRuteFinderFactory OSGi service.
 */
public class NavigationActivator implements BundleActivator {
    
    private static final Logger log = LoggerFactory.getLogger(NavigationActivator.class);
    
    private ServiceRegistration<IRuteFinderFactory> factoryRegistration;
    
    @Override
    public void start(BundleContext context) throws Exception {
        log.info("Starting sokybot-game-navigation bundle");
        
        RuteFinderFactoryImpl factory = new RuteFinderFactoryImpl();
        factoryRegistration = context.registerService(IRuteFinderFactory.class, factory, null);
        
        log.info("IRuteFinderFactory service registered");
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("Stopping sokybot-game-navigation bundle");
        
        if (factoryRegistration != null) {
            factoryRegistration.unregister();
            factoryRegistration = null;
        }
        
        log.info("sokybot-game-navigation bundle stopped");
    }
}
