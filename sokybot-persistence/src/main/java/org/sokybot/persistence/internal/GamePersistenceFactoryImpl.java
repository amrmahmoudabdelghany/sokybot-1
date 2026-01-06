package org.sokybot.persistence.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;

/**
 * Factory implementation for managing per-game persistence contexts.
 * Creates separate EntityManagerFactory for each registered game,
 * backed by per-game H2 database files.
 */
@Component(service = IGamePersistenceFactory.class)
public class GamePersistenceFactoryImpl implements IGamePersistenceFactory {
    
    private final Map<String, EntityManagerFactory> emfMap = new HashMap<>();
    private final Map<String, IGameDataLookup> lookupMap = new HashMap<>();
    
    @Override
    public synchronized IGameDataLookup registerGame(String gamePath) {
        // Check if already registered
        if (lookupMap.containsKey(gamePath)) {
            return lookupMap.get(gamePath);
        }
        
        // Create per-game EntityManagerFactory
        Properties props = new Properties();
        props.put("javax.persistence.jdbc.driver", "org.h2.Driver");
        props.put("javax.persistence.jdbc.url", "jdbc:h2:file:./" + gamePath.hashCode());
        props.put("javax.persistence.jdbc.user", "sa");
        props.put("javax.persistence.jdbc.password", "password");
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(
            "sokybot-game-persistence", props);
        emfMap.put(gamePath, emf);
        
        // Create lookup service
        IGameDataLookup lookup = new GameDataLookupImpl(gamePath, emf);
        lookupMap.put(gamePath, lookup);
        
        System.out.println("PERSISTENCE: Registered game: " + gamePath);
        return lookup;
    }
    
    @Override
    public IGameDataLookup getLookup(String gamePath) {
        return lookupMap.get(gamePath);
    }
    
    @Override
    public synchronized void unregisterGame(String gamePath) {
        lookupMap.remove(gamePath);
        
        EntityManagerFactory emf = emfMap.remove(gamePath);
        if (emf != null && emf.isOpen()) {
            emf.close();
            System.out.println("PERSISTENCE: Unregistered game: " + gamePath);
        }
    }
    
    @Deactivate
    public void deactivate() {
        // Close all EMFs when bundle stops
        for (EntityManagerFactory emf : emfMap.values()) {
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
        }
        emfMap.clear();
        lookupMap.clear();
        System.out.println("PERSISTENCE: All game contexts cleaned up");
    }
}
