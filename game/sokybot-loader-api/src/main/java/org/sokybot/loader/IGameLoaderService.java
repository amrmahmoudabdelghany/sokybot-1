package org.sokybot.loader;

import java.util.Optional;
import java.util.Set;

/**
 * Service for discovering and managing IGameLoader implementations.
 */
public interface IGameLoaderService {

    /**
     * List all available game loader names.
     * 
     * @return set of available loader names
     */
    Set<String> listAvailables();

    /**
     * Find a game loader by its unique name.
     * 
     * @param name the loader name
     * @return optional containing the loader if found
     */
    Optional<IGameLoader> findGameLoader(String name);
}
