package org.sokybot.game.navigation.internal;

import org.sokybot.game.navigation.IRuteFinder;
import org.sokybot.game.navigation.IRuteFinderFactory;
import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Factory implementation for creating RuteFinder instances.
 */
public class RuteFinderFactoryImpl implements IRuteFinderFactory {
    
    @Override
    public IRuteFinder createRuteFinder(IGameDataLookup gameDataLookup) {
        NavMesh navMesh = new NavMesh(gameDataLookup);
        return new RuteFinder(navMesh, gameDataLookup);
    }
}
