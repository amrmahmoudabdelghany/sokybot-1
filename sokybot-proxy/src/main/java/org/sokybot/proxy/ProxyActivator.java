package org.sokybot.proxy;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGi Bundle Activator for the proxy module.
 * Registers IProxyConnectionFactory as an OSGi service.
 */
public class ProxyActivator implements BundleActivator {
    
    private ServiceRegistration<IProxyConnectionFactory> registration;
    private ProxyConnectionFactory factory;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Sokybot Proxy: Starting...");
        
        factory = new ProxyConnectionFactory();
        registration = context.registerService(IProxyConnectionFactory.class, factory, null);
        
        System.out.println("Sokybot Proxy: IProxyConnectionFactory registered as OSGi service");
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("Sokybot Proxy: Stopping...");
        
        if (registration != null) {
            registration.unregister();
        }
        
        if (factory != null) {
            factory.shutdown();
        }
        
        System.out.println("Sokybot Proxy: Stopped");
    }
}
