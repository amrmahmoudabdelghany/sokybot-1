package org.sokybot.persistence.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManagerFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;
import org.sokybot.persistence.service.IPersistenceContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of IGamePersistenceFactory.
 * Manages per-game data lookups using IPersistenceContextManager.
 */
@Component(service = IGamePersistenceFactory.class, immediate = true)
public class GamePersistenceFactoryImpl implements IGamePersistenceFactory {

    private static final Logger log = LoggerFactory.getLogger(GamePersistenceFactoryImpl.class);
    private IPersistenceContextManager contextManager;
    private final Map<String, IGameDataLookup> lookups = new ConcurrentHashMap<>();
    private final ThreadLocal<java.util.Set<String>> initializingPaths = ThreadLocal
            .withInitial(java.util.HashSet::new);

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

        IGameDataLookup lookup = lookups.get(gamePath);
        if (lookup != null)
            return lookup;

        synchronized (lookups) {
            lookup = lookups.get(gamePath);
            if (lookup != null)
                return lookup;

            // Recursion protection
            java.util.Set<String> currentInit = initializingPaths.get();
            if (currentInit.contains(gamePath)) {
                log.warn(
                        "Circular dependency detected during persistence initialization for: {}. Returning partial lookup.",
                        gamePath);
                return null;
            }

            currentInit.add(gamePath);
            try {
                log.info("Registering game persistence context for: {}", gamePath);
                EntityManagerFactory emf = contextManager.getEntityManagerFactory(gamePath);
                lookup = new GameDataLookupImpl(gamePath, emf);
                lookups.put(gamePath, lookup);
                return lookup;
            } finally {
                currentInit.remove(gamePath);
            }
        }
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
