package org.sokybot.proxy;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;

/**
 * OSGi Bundle Activator for the proxy module.
 * Registers IProxyConnectionFactory as an OSGi service.
 */
public class ProxyActivator implements BundleActivator {
    
    private ServiceRegistration<IProxyConnectionFactory> registration;
    private ProxyConnectionFactory factory;
    private ServiceReference<EventAdmin> eventAdminRef;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("Sokybot Proxy: Starting...");
        
        eventAdminRef = context.getServiceReference(EventAdmin.class);
        EventAdmin eventAdmin = null;
        if (eventAdminRef != null) {
            eventAdmin = context.getService(eventAdminRef);
        } else {
            System.err.println("Sokybot Proxy: EventAdmin service not found!");
        }
        
        factory = new ProxyConnectionFactory(eventAdmin);
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
        
        if (eventAdminRef != null) {
            context.ungetService(eventAdminRef);
        }
        
        System.out.println("Sokybot Proxy: Stopped");
    }
}

