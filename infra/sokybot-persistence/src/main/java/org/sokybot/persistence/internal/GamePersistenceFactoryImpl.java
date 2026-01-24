package org.sokybot.persistence.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManagerFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.persistence.service.IPersistenceContextManager;

/**
 * Implementation of IGamePersistenceFactory.
 * Manages per-game data lookups using IPersistenceContextManager.
 */
@Component(service = IGamePersistenceFactory.class, immediate = true)
public class GamePersistenceFactoryImpl implements IGamePersistenceFactory {

    private IPersistenceContextManager contextManager;
    private final Map<String, IGameDataLookup> lookups = new ConcurrentHashMap<>();

    /**
     * OSGi DS default constructor.
     */
    public GamePersistenceFactoryImpl() {
    }

    /**
     * Constructor for programmatic creation.
     */
    public GamePersistenceFactoryImpl(IPersistenceContextManager contextManager) {
        this.contextManager = contextManager;
    }

    @Reference
    public void setPersistenceContextManager(IPersistenceContextManager contextManager) {
        this.contextManager = contextManager;
    }

    @Override
    public IGameDataLookup registerGame(String gamePath) {
        if (contextManager == null) {
            throw new IllegalStateException("PersistenceContextManager not set");
        }
        
        return lookups.computeIfAbsent(gamePath, path -> {
            EntityManagerFactory emf = contextManager.getEntityManagerFactory(path);
            return new GameDataLookupImpl(path, emf);
        });
    }

    @Override
    public IGameDataLookup getLookup(String gamePath) {
        return lookups.get(gamePath);
    }

    @Override
    public void unregisterGame(String gamePath) {
        IGameDataLookup lookup = lookups.remove(gamePath);
        if (lookup != null && contextManager != null) {
            contextManager.closeEntityManagerFactory(gamePath);
        }
    }
}
