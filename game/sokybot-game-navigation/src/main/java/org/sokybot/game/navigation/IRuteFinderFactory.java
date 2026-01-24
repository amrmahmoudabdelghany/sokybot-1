package org.sokybot.game.navigation;

import org.sokybot.persistence.service.IGameDataLookup;

/**
 * Factory for creating IRuteFinder instances.
 * Published as OSGi service for cross-module access.
 */
public interface IRuteFinderFactory {
    
    /**
     * Create a new route finder for the given game data lookup.
     * 
     * @param gameDataLookup the data source for navigation mesh data
     * @return a new IRuteFinder instance
     */
    IRuteFinder createRuteFinder(IGameDataLookup gameDataLookup);
}
