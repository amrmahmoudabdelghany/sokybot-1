package org.sokybot.persistence;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Hashtable;

import org.sokybot.persistence.internal.GamePersistenceFactoryImpl;
import org.sokybot.persistence.service.IGamePersistenceFactory;

public class PersistenceActivator implements BundleActivator {
    
    private EntityManagerFactory emf;
    private ServiceRegistration<EntityManagerFactory> emfRegistration;
    private ServiceRegistration<IGamePersistenceFactory> persistenceFactoryRegistration;
    
    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("PERSISTENCE: Starting persistence bundle...");
        
        try {
            // Create EntityManagerFactory from persistence.xml
            emf = Persistence.createEntityManagerFactory("sokybot-persistence-unit");
            System.out.println("PERSISTENCE: EntityManagerFactory created successfully");
            
            // Register as OSGi service
            Hashtable<String, Object> props = new Hashtable<>();
            props.put("persistence.unit.name", "sokybot-persistence-unit");
            
            emfRegistration = context.registerService(
                EntityManagerFactory.class, 
                emf, 
                props
            );
            
            System.out.println("PERSISTENCE: EntityManagerFactory registered as OSGi service");
            
            // Register GamePersistenceFactory
            IGamePersistenceFactory factory = new GamePersistenceFactoryImpl(emf);
            persistenceFactoryRegistration = context.registerService(
                IGamePersistenceFactory.class,
                factory,
                new Hashtable<>()
            );
            System.out.println("PERSISTENCE: GamePersistenceFactory registered as OSGi service");

        } catch (Exception e) {
            System.err.println("PERSISTENCE: Failed to initialize EntityManagerFactory");
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("PERSISTENCE: Stopping persistence bundle...");
        
        // Unregister service
        if (emfRegistration != null) {
            emfRegistration.unregister();
        }
        if (persistenceFactoryRegistration != null) {
            persistenceFactoryRegistration.unregister();
        }
        
        // Close EntityManagerFactory
        if (emf != null && emf.isOpen()) {
            emf.close();
            System.out.println("PERSISTENCE: EntityManagerFactory closed");
        }
    }
}
