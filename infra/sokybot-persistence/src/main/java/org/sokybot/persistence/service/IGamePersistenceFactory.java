package org.sokybot.persistence.service;

/**
 * Factory for managing per-game persistence contexts.
 * Creates and manages IGameDataLookup instances for each game.
 * Published as OSGi service for cross-module access.
 */
public interface IGamePersistenceFactory {
    
    /**
     * Register a game and create its lookup service.
     * Called when MachineGroup starts up.
     * 
     * @param gamePath The game installation path (unique identifier)
     * @return IGameDataLookup for this game's entity access
     */
    IGameDataLookup registerGame(String gamePath);
    
    /**
     * Get existing lookup for a registered game.
     * 
     * @param gamePath The game installation path
     * @return IGameDataLookup or null if not registered
     */
    IGameDataLookup getLookup(String gamePath);
    
    /**
     * Unregister a game when MachineGroup shuts down.
     * Releases resources associated with this game.
     * 
     * @param gamePath The game installation path
     */
    void unregisterGame(String gamePath);
}
