package org.sokybot.persistence.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManagerFactory;

import org.sokybot.persistence.service.IGameDataLookup;
import org.sokybot.persistence.service.IGamePersistenceFactory;

public class GamePersistenceFactoryImpl implements IGamePersistenceFactory {

    private final EntityManagerFactory emf;
    private final Map<String, IGameDataLookup> lookups = new ConcurrentHashMap<>();

    public GamePersistenceFactoryImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public IGameDataLookup registerGame(String gamePath) {
        return lookups.computeIfAbsent(gamePath, path -> new GameDataLookupImpl(path, emf));
    }

    @Override
    public IGameDataLookup getLookup(String gamePath) {
        return lookups.get(gamePath);
    }

    @Override
    public void unregisterGame(String gamePath) {
        lookups.remove(gamePath);
    }
}
